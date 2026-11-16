import Shared
import SwiftUI

/// RF-126: the sheet's ViewModel, held by the screen that opened it so "Undo" still works after the sheet closes.
@MainActor
final class MarkReadingModel: ObservableObject, Identifiable {
    let id: Int64
    let screen: ScreenModel<MarkReadingViewModel, NSArray>
    private var change: Any?

    init(mangaId: Int64) {
        id = mangaId
        screen = ScreenModel(Core.graph.markReadingViewModel(mangaId: mangaId)) { IosBridge.shared.markReadingScreen(viewModel: $0) }
        change = screen.objectWillChange.sink { [weak self] _ in self?.objectWillChange.send() }
    }

    var state: MarkReadingViewModelState? { screen.state.firstObject as? MarkReadingViewModelState }
    var viewModel: MarkReadingViewModel { screen.viewModel }
}

/// SCR-LIB-004 (HIG): nothing, up to a chapter or all, with a stepper and a slider; a work without its chapter list
/// asks its source first.
struct MarkReadingSheet: View {
    @ObservedObject var model: MarkReadingModel
    let onSaved: (MarkReadingViewModel.Saved) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                content
            }
            .navigationTitle(L("mark_reading"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(L("action_cancel")) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L("action_save")) {
                        model.viewModel.save { saved in
                            onSaved(saved)
                            dismiss()
                        }
                    }
                    .disabled(!(model.state is MarkReadingViewModelStateReady))
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    @ViewBuilder
    private var content: some View {
        if let ready = model.state as? MarkReadingViewModelStateReady {
            Section {
                Picker(L("mark_reading"), selection: Binding(
                    get: { Int(IosBridge.shared.markKind(mark: ready.mark)) },
                    set: { kind in choose(kind, ready) }
                )) {
                    Text(L("mark_reading_none")).tag(0)
                    Text(L("mark_reading_up_to")).tag(1)
                    Text(L("mark_reading_all")).tag(2)
                }
                .pickerStyle(.inline)
                .labelsHidden()
            } footer: {
                Text(L("mark_reading_chapters", ready.title, Int(ready.total)))
            }
            if IosBridge.shared.markKind(mark: ready.mark) == 1, let chapter = ready.chapter {
                Section {
                    Stepper(value: Binding(get: { Int(ready.index) }, set: { model.viewModel.chooseUpTo(index: Int32($0)) }),
                            in: 0...max(ready.chapters.count - 1, 0)) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(L("manga_chapter_label", IosBridge.shared.chapterNumber(chapter: chapter)))
                            if let title = chapter.source.title {
                                Text(title).font(.footnote).foregroundStyle(.secondary)
                            }
                        }
                    }
                    if ready.chapters.count > 1 {
                        Slider(value: Binding(get: { Double(ready.index) }, set: { model.viewModel.chooseUpTo(index: Int32($0.rounded())) }),
                               in: 0...Double(ready.chapters.count - 1), step: 1) {
                            Text(L("mark_reading_slider"))
                        } minimumValueLabel: {
                            Text(L("chapter_short", IosBridge.shared.chapterNumber(chapter: ready.chapters.first!))).font(.caption)
                        } maximumValueLabel: {
                            Text(L("chapter_short", IosBridge.shared.chapterNumber(chapter: ready.chapters.last!))).font(.caption)
                        }
                        .accessibilityValue(L("chapter_short", IosBridge.shared.chapterNumber(chapter: chapter)))
                    }
                } footer: {
                    Text(L("mark_reading_up_to_hint") + ". " + L("mark_reading_note"))
                }
            } else {
                Section {} footer: { Text(L("mark_reading_note")) }
            }
        } else if let failed = model.state as? MarkReadingViewModelStateFailed {
            Section {
                ErrorBox(message: failed.error.message, action: L("action_retry")) { model.viewModel.retry() }
            }
        } else {
            Section {
                HStack(spacing: 8) {
                    ProgressView()
                    Text(L("mark_reading_loading", model.state?.title ?? "")).foregroundStyle(.secondary)
                }
            }
        }
    }

    private func choose(_ kind: Int, _ ready: MarkReadingViewModelStateReady) {
        switch kind {
        case 0: model.viewModel.choose(mark: IosBridge.shared.markNone())
        case 2: model.viewModel.choose(mark: IosBridge.shared.markAll())
        default: model.viewModel.chooseUpTo(index: ready.index)
        }
    }
}

/// The message after saving, with "Undo" (RF-126).
func markReadingDone(_ saved: MarkReadingViewModel.Saved) -> String {
    switch IosBridge.shared.markKind(mark: saved.mark) {
    case 0: return L("mark_reading_done_none", saved.title)
    case 2: return L("mark_reading_done_all", saved.title)
    default:
        let number = saved.chapterNumber.map { IosBridge.shared.chapterLabel(number: $0.doubleValue) } ?? ""
        return L("mark_reading_done_up_to", saved.title, number)
    }
}

/// RF-127: the button on a result's cover: puts the work in the library, or a filled ✓ when it already is.
struct LibraryToggleButton: View {
    let title: String
    let inLibrary: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: inLibrary ? "checkmark" : "bookmark")
                .font(.system(size: 15, weight: .bold))
                .overlay(alignment: .center) {
                    if !inLibrary { Image(systemName: "plus").font(.system(size: 8, weight: .heavy)).offset(y: -1) }
                }
                .foregroundStyle(inLibrary ? Palette.onPrimary : Self.ink)
                .frame(width: 34, height: 34)
                .background(inLibrary ? Palette.primary : Self.paper, in: Circle())
                .overlay(Circle().stroke(inLibrary ? Self.paper : Self.ink, lineWidth: 2))
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(inLibrary ? L("browse_in_library_named", title) : L("browse_add_named", title))
    }

    private static let paper = Color(red: 1, green: 0xFD / 255, blue: 0xF7 / 255)
    private static let ink = Color(red: 0x16 / 255, green: 0x16 / 255, blue: 0x1A / 255)
}

/// A message at the bottom of a screen with one action ("Marcar leitura", "Desfazer"), gone after 5 s.
struct ActionBar: View {
    let text: String
    let actionLabel: String
    let action: () -> Void
    let dismiss: () -> Void

    var body: some View {
        HStack {
            Text(text).font(.subheadline).lineLimit(2)
            Spacer()
            Button(actionLabel, action: action).font(.subheadline.weight(.semibold))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Palette.surfaceHighest, in: RoundedRectangle(cornerRadius: Metrics.shapeChip, style: .continuous))
        .padding(.horizontal, Metrics.spaceMarginCompact)
        .padding(.bottom, 8)
        .task {
            try? await Task.sleep(nanoseconds: 5_000_000_000)
            dismiss()
        }
    }
}

/// RF-129: "LIDA!" on a work read to the end, a bar at the foot of the cover while it is partly read.
struct ReadingOnCover: View {
    let item: Shared.LibraryItem

    var body: some View {
        let total = Int(item.totalChapters)
        let read = Int(IosBridge.shared.readCount(item: item))
        if total > 0 && item.unread == 0 {
            ZStack {
                Color.black.opacity(0.45)
                Text(L("library_cover_done"))
                    .font(ComicFont.bangers(20, relativeTo: .headline))
                    .foregroundStyle(Self.ink)
                    .padding(.horizontal, 8).padding(.vertical, 2)
                    .background(Self.paper)
                    .overlay(Rectangle().stroke(Self.ink, lineWidth: 2))
                    .rotationEffect(.degrees(-8))
            }
        } else if total > 0 && read > 0 && read < total {
            VStack {
                Spacer()
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Rectangle().fill(Color.black.opacity(0.55))
                        Rectangle().fill(Self.bar).frame(width: geo.size.width * CGFloat(read) / CGFloat(total))
                    }
                }
                .frame(height: 6)
            }
            .accessibilityElement()
            .accessibilityLabel(L("library_cover_read", read, total))
        }
    }

    private static let paper = Color(red: 0xF6 / 255, green: 0xF1 / 255, blue: 0xE7 / 255)
    private static let ink = Color(red: 0x16 / 255, green: 0x16 / 255, blue: 0x1A / 255)
    /// The light violet on the dark track reads on any cover (docs/tokens.json, primary in the dark scheme).
    private static let bar = Color(red: 0xC7 / 255, green: 0xBF / 255, blue: 0xFF / 255)
}

#Preview("LibraryToggleButton · Light") {
    HStack { LibraryToggleButton(title: "Kaiju", inLibrary: false) {}; LibraryToggleButton(title: "Kaiju", inLibrary: true) {} }
        .padding().preferredColorScheme(.light)
}
#Preview("LibraryToggleButton · Dark") {
    HStack { LibraryToggleButton(title: "Kaiju", inLibrary: false) {}; LibraryToggleButton(title: "Kaiju", inLibrary: true) {} }
        .padding().preferredColorScheme(.dark)
}
#Preview("ActionBar · Light") { ActionBar(text: "Kaiju Express", actionLabel: L("mark_reading"), action: {}, dismiss: {}).preferredColorScheme(.light) }
#Preview("ActionBar · Dark") { ActionBar(text: "Kaiju Express", actionLabel: L("mark_reading"), action: {}, dismiss: {}).preferredColorScheme(.dark) }
#Preview("MarkReadingSheet · Light") { MarkReadingSheet(model: MarkReadingModel(mangaId: 1)) { _ in }.previewScreen().preferredColorScheme(.light) }
#Preview("MarkReadingSheet · Dark") { MarkReadingSheet(model: MarkReadingModel(mangaId: 1)) { _ in }.previewScreen().preferredColorScheme(.dark) }
#Preview("ReadingOnCover · Light") { ReadingOnCoverPreview().preferredColorScheme(.light) }
#Preview("ReadingOnCover · Dark") { ReadingOnCoverPreview().preferredColorScheme(.dark) }

private struct ReadingOnCoverPreview: View {
    var body: some View {
        HStack(spacing: 8) {
            ForEach([(22, 120), (30, 30)], id: \.0) { read, total in
                ZStack {
                    Color(red: 0.17, green: 0.23, blue: 0.29)
                    ReadingOnCover(item: IosBridge.shared.sampleLibraryItem(title: "Lâmina do Norte", read: Int32(read), total: Int32(total)))
                }
                .frame(width: 110, height: 160)
            }
        }
        .padding()
    }
}
