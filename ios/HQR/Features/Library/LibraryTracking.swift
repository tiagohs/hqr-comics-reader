import Shared
import SwiftUI

/// RF-132: the order of the bar and the chips: what asks for attention first.
let shelfOrder: [ReadingState] = [.reading, .theNewChapters, .upToDate, .finished, .pending]

extension ReadingState {
    var labelKey: String {
        switch self {
        case .pending: "library_state_pending"
        case .reading: "library_state_reading"
        case .theNewChapters: "library_state_new"
        case .upToDate: "library_state_up_to_date"
        default: "library_state_finished"
        }
    }

    /// docs/tokens.json color.comic.state-*; "Lidas" takes the primary, "Pendentes" a neutral ink.
    var color: Color {
        switch self {
        case .pending: Palette.onSurface.opacity(0.3)
        case .reading: TrackingColors.reading
        case .theNewChapters: TrackingColors.new
        case .upToDate: TrackingColors.upToDate
        default: Palette.primary
        }
    }
}

enum TrackingColors {
    static let reading = Color(red: 0xF2 / 255, green: 0xC1 / 255, blue: 0x4E / 255)
    static let upToDate = Color(red: 0x7F / 255, green: 0xD1 / 255, blue: 0xAE / 255)
    static let upToDateInk = Color(red: 0x0B / 255, green: 0x2A / 255, blue: 0x1E / 255)
    static let new = Color(red: 1, green: 0xF1 / 255, blue: 0xA8 / 255)
    static let ink = Color(red: 0x16 / 255, green: 0x16 / 255, blue: 0x1A / 255)
    static let paper = Color(red: 0xF6 / 255, green: 0xF1 / 255, blue: 0xE7 / 255)
}

/// RF-132: the large ring with the % read, the two numbers as cards below, the bar by situation and its buttons,
/// which open the shelf in that situation; "Tudo" adds HQs and Mangás apart.
struct ProgressCard: View {
    let progress: ProgressSummary
    var comics: ProgressSummary? = nil
    var manga: ProgressSummary? = nil
    var selected: ReadingState? = nil
    var onSelect: (ReadingState?) -> Void = { _ in }

    private var parts: [(ReadingState, Int)] {
        shelfOrder.compactMap { state in
            let count = Int(IosBridge.shared.stateCount(progress: progress, state: state))
            return count > 0 ? (state, count) : nil
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            ring.frame(maxWidth: .infinity)
            HStack(spacing: 8) {
                kpi(value: "\(progress.worksRead)", suffix: "/\(progress.works)", label: L("library_progress_works"))
                kpi(value: "\(progress.chaptersRead)", suffix: "", label: L("library_progress_chapters"))
            }
            if !parts.isEmpty {
                GeometryReader { geo in
                    let total = CGFloat(parts.reduce(0) { $0 + $1.1 })
                    let gaps = CGFloat(parts.count - 1) * 2
                    HStack(spacing: 2) {
                        ForEach(parts, id: \.0) { state, count in
                            state.color.frame(width: (geo.size.width - gaps) * CGFloat(count) / total)
                        }
                    }
                }
                .frame(height: 12)
                .clipShape(Capsule())
                .accessibilityElement()
                .accessibilityLabel(parts.map { "\($0.1) \(L($0.0.labelKey))" }.joined(separator: ", "))
                StateButtons(parts: parts, selected: selected, onSelect: onSelect)
                    .accessibilityElement(children: .contain)
                    .accessibilityLabel(L("library_state_group"))
            }
            if let comics, let manga {
                VStack(spacing: 6) {
                    typeRow(L("library_area_comics"), comics)
                    typeRow(L("library_area_manga"), manga)
                }
            }
        }
        .padding(20)
        .background(Palette.surfaceHigh, in: RoundedRectangle(cornerRadius: 28, style: .continuous))
        .accessibilityElement(children: .contain)
        .accessibilityLabel(L("library_progress_title"))
    }

    private var ring: some View {
        ZStack {
            Circle().stroke(Palette.onSurface.opacity(0.14), lineWidth: 10)
            Circle()
                .trim(from: 0, to: CGFloat(progress.percent) / 100)
                .stroke(Palette.primary, style: StrokeStyle(lineWidth: 14, lineCap: .round))
                .rotationEffect(.degrees(-90))
            VStack(spacing: 0) {
                Text("\(progress.percent)%").font(.system(size: 44, weight: .heavy, design: .rounded))
                Text(L("library_progress_read")).font(.subheadline.weight(.semibold)).foregroundStyle(Palette.onSurfaceVariant)
            }
        }
        .frame(width: 176, height: 176)
        .accessibilityElement()
        .accessibilityLabel(L("library_progress_ring", "\(progress.percent)%"))
    }

    private func kpi(value: String, suffix: String, label: String) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .firstTextBaseline, spacing: 2) {
                Text(value).font(.system(size: 28, weight: .heavy, design: .rounded))
                if !suffix.isEmpty { Text(suffix).font(.headline).foregroundStyle(Palette.onSurfaceVariant) }
            }
            Text(label).font(.subheadline.weight(.semibold)).foregroundStyle(Palette.onSurfaceVariant)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 14).padding(.vertical, 12)
        .background(Palette.onSurface.opacity(0.07), in: RoundedRectangle(cornerRadius: Metrics.shapeCard, style: .continuous))
        .accessibilityElement(children: .combine)
    }

    private func typeRow(_ name: String, _ summary: ProgressSummary) -> some View {
        HStack(spacing: 12) {
            Text(name).font(.subheadline.weight(.bold)).frame(width: 64, alignment: .leading)
            ProgressView(value: Double(summary.percent), total: 100)
            Text("\(summary.worksRead)/\(summary.works)").font(.footnote.weight(.semibold)).foregroundStyle(Palette.onSurfaceVariant)
        }
        .frame(minHeight: 28)
        .accessibilityElement()
        .accessibilityLabel(L("library_progress_type", name, Int(summary.worksRead), Int(summary.works)))
        .opacity(summary.works == 0 ? 0 : 1)
    }
}

/// RF-132: one bordered button per situation; the chosen one is prominent and opens the shelf filtered.
struct StateButtons: View {
    let parts: [(ReadingState, Int)]
    var selected: ReadingState?
    var allCount: Int? = nil
    var onSelect: (ReadingState?) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                if let allCount {
                    toggle(label: "\(L("library_state_all")) \(allCount)", on: selected == nil, dot: nil) { onSelect(nil) }
                }
                ForEach(parts, id: \.0) { state, count in
                    toggle(label: "\(L(state.labelKey)) \(count)", on: selected == state, dot: state.color) {
                        onSelect(selected == state && allCount == nil ? nil : state)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func toggle(label: String, on: Bool, dot: Color?, action: @escaping () -> Void) -> some View {
        let content = HStack(spacing: 6) {
            if on {
                Image(systemName: "checkmark").font(.caption.weight(.bold))
            } else if let dot {
                Circle().fill(dot).frame(width: 10, height: 10)
            }
            Text(label)
        }
        if on {
            Button(action: action) { content }.buttonStyle(.borderedProminent).accessibilityAddTraits(.isSelected)
        } else {
            Button(action: action) { content }.buttonStyle(.bordered).tint(Palette.onSurface)
        }
    }
}

/// RF-133: the last readings of the area, paged sideways; the first is the one to resume.
struct ContinueCarousel: View {
    let entries: [HistoryEntry]
    var onOpen: (HistoryEntry) -> Void = { _ in }

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            LazyHStack(spacing: 8) {
                ForEach(entries, id: \.manga.id) { entry in
                    Button { onOpen(entry) } label: { item(entry) }
                        .buttonStyle(.plain)
                        .containerRelativeFrame(.horizontal) { width, _ in width * 0.8 }
                }
            }
            .scrollTargetLayout()
        }
        .scrollTargetBehavior(.viewAligned)
        .contentMargins(.horizontal, Metrics.spaceMarginCompact, for: .scrollContent)
        .frame(height: 220)
    }

    private func item(_ entry: HistoryEntry) -> some View {
        let again = entry.manga.isRereading ? L("library_reading_number", Int(entry.manga.timesRead) + 1) + " · " : ""
        let line = again + L("manga_continue_chapter", entry.chapter.numberText, Int(entry.chapter.lastPage) + 1)
        return ZStack(alignment: .bottomLeading) {
            CoverImage(manga: entry.manga.source)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .clipped()
            LinearGradient(colors: [.clear, .clear, .black.opacity(0.82)], startPoint: .top, endPoint: .bottom)
            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(entry.manga.source.title).font(.headline).lineLimit(2)
                    Text(line).font(.footnote.weight(.semibold)).lineLimit(1)
                }
                .foregroundStyle(TrackingColors.paper)
                Spacer()
                Image(systemName: "play.fill")
                    .font(.title3)
                    .frame(width: 48, height: 48)
                    .background(Palette.primary, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .foregroundStyle(Palette.onPrimary)
            }
            .padding(16)
        }
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(entry.manga.source.title), \(line)")
        .accessibilityHint(L("action_continue"))
    }
}

/// RF-132: works whose source added chapters after a full reading.
struct NewChaptersSection: View {
    let items: [Shared.LibraryItem]
    var onOpen: (Shared.LibraryItem) -> Void = { _ in }
    var onSeeAll: () -> Void = {}

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(L("library_state_new")).font(.title3.weight(.bold)).accessibilityAddTraits(.isHeader)
                Spacer()
                Button(L("library_state_all"), action: onSeeAll)
            }
            VStack(spacing: 0) {
                ForEach(Array(items.prefix(3).enumerated()), id: \.element.manga.id) { index, item in
                    if index > 0 { Divider().padding(.leading, 76) }
                    Button { onOpen(item) } label: {
                        HStack(spacing: 16) {
                            CoverImage(manga: item.manga.source, width: 44)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(item.manga.source.title).font(.body.weight(.semibold)).lineLimit(1)
                                Text(L("library_new_line", Int(item.unread))).font(.subheadline).foregroundStyle(Palette.onSurfaceVariant)
                            }
                            Spacer()
                            CornerPill(text: L("library_new_count", Int(item.unread)), background: TrackingColors.new, content: TrackingColors.ink)
                        }
                        .padding(.horizontal, 16).padding(.vertical, 8)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
            .background(Palette.surfaceHigh, in: RoundedRectangle(cornerRadius: Metrics.shapeCard, style: .continuous))
        }
    }
}

/// RF-134: the characters found in the HQs; one at a time filters the whole area.
struct CharacterButtons: View {
    let names: [String]
    let counts: [Int]
    var selected: String?
    var onSelect: (String?) -> Void = { _ in }
    var onEdit: () -> Void = {}

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(L("library_characters")).font(.subheadline.weight(.bold)).foregroundStyle(Palette.onSurfaceVariant)
                Spacer()
                Button(L("library_characters_edit"), action: onEdit).font(.subheadline.weight(.semibold))
            }
            .padding(.horizontal, Metrics.spaceMarginCompact)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    chip(L("library_characters_all"), count: nil, on: selected == nil) { onSelect(nil) }
                    ForEach(Array(names.enumerated()), id: \.element) { index, name in
                        chip(name, count: counts[index], on: selected == name) { onSelect(selected == name ? nil : name) }
                    }
                }
                .padding(.horizontal, Metrics.spaceMarginCompact)
            }
        }
    }

    @ViewBuilder
    private func chip(_ name: String, count: Int?, on: Bool, action: @escaping () -> Void) -> some View {
        let content = HStack(spacing: 6) {
            Text(String(name.prefix(1)))
                .font(.caption2.weight(.heavy))
                .frame(width: 22, height: 22)
                .background(on ? Palette.onPrimary.opacity(0.25) : Palette.onSurface.opacity(0.14), in: Circle())
            Text(count.map { "\(name) \($0)" } ?? name)
        }
        if on {
            Button(action: action) { content }.buttonStyle(.borderedProminent).buttonBorderShape(.capsule).accessibilityAddTraits(.isSelected)
        } else {
            Button(action: action) { content }.buttonStyle(.bordered).buttonBorderShape(.capsule).tint(Palette.onSurface)
        }
    }
}

/// RF-134: the list of characters, to add or take out (swipe to remove).
struct CharactersSheet: View {
    let names: [String]
    var onAdd: (String) -> Void = { _ in }
    var onRemove: (String) -> Void = { _ in }
    @State private var text = ""
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    HStack {
                        TextField(L("library_characters_new"), text: $text).submitLabel(.done).onSubmit(add)
                        Button(L("library_characters_add"), action: add).disabled(text.trimmingCharacters(in: .whitespaces).isEmpty)
                    }
                } footer: { Text(L("library_characters_hint")) }
                Section {
                    ForEach(names, id: \.self) { name in
                        Text(name).swipeActions {
                            Button(role: .destructive) { onRemove(name) } label: {
                                Label(L("library_characters_remove", name), systemImage: "trash")
                            }
                        }
                    }
                }
            }
            .navigationTitle(L("library_characters_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button(L("action_ok")) { dismiss() } } }
        }
    }

    private func add() {
        onAdd(text)
        text = ""
    }
}

/// RF-132/RF-135: the mark at the corner of a cover: % read, "+3", ✓, "2×" or "2ª · 28%".
struct StateCorner: View {
    let item: Shared.LibraryItem

    var body: some View {
        let state = LibraryProgress.shared.state(item: item)
        let total = Int(item.totalChapters)
        let read = Int(IosBridge.shared.readCount(item: item))
        let again = Int(IosBridge.shared.readingNumber(item: item))
        let times = Int(IosBridge.shared.timesFinished(item: item))
        Group {
            if again > 0 {
                let at = min(100, Int(IosBridge.shared.rereadChapter(manga: item.manga) * 100 / Double(max(total, 1))))
                CornerPill(text: L("library_reading_corner", again, "\(at)%"), background: TrackingColors.ink.opacity(0.8), content: TrackingColors.paper)
            } else if state == .theNewChapters {
                CornerPill(text: L("library_new_count", Int(item.unread)), background: TrackingColors.new, content: TrackingColors.ink)
            } else if state == .upToDate || state == .finished {
                let background = state == .finished ? Palette.primary : TrackingColors.upToDate
                let content = state == .finished ? Palette.onPrimary : TrackingColors.upToDateInk
                if times > 1 {
                    CornerPill(text: L("library_times_read", times), background: background, content: content)
                        .accessibilityLabel(L("library_times_read_label", times))
                } else {
                    Image(systemName: "checkmark")
                        .font(.caption.weight(.heavy))
                        .frame(width: 24, height: 24)
                        .background(background, in: Circle())
                        .foregroundStyle(content)
                        .accessibilityLabel(L(state.labelKey))
                }
            } else if state == .reading && total > 0 && read > 0 {
                CornerPill(text: "\(read * 100 / total)%", background: TrackingColors.ink.opacity(0.8), content: TrackingColors.paper)
            }
        }
        .padding(.trailing, 6)
        .padding(.bottom, 12)
    }
}

struct CornerPill: View {
    let text: String
    let background: Color
    let content: Color

    var body: some View {
        Text(text)
            .font(.caption.weight(.heavy))
            .lineLimit(1)
            .padding(.horizontal, 8).padding(.vertical, 3)
            .background(background, in: Capsule())
            .foregroundStyle(content)
    }
}

#Preview("ProgressCard · Light") {
    ProgressCard(progress: IosBridge.shared.sampleProgress(), comics: IosBridge.shared.sampleProgress(), manga: IosBridge.shared.sampleProgress())
        .padding().preferredColorScheme(.light)
}
#Preview("ProgressCard · Dark") {
    ProgressCard(progress: IosBridge.shared.sampleProgress(), selected: .reading).padding().preferredColorScheme(.dark)
}
#Preview("StateButtons · Light") {
    StateButtons(parts: [(.reading, 8), (.upToDate, 4)], selected: nil, allCount: 12) { _ in }.padding().preferredColorScheme(.light)
}
#Preview("StateButtons · Dark") {
    StateButtons(parts: [(.reading, 8), (.upToDate, 4)], selected: .reading, allCount: 12) { _ in }.padding().preferredColorScheme(.dark)
}
#Preview("ContinueCarousel · Light") {
    ContinueCarousel(entries: ["Lâmina do Norte", "Ronin 2099"].map { IosBridge.shared.sampleHistory(title: $0) }).preferredColorScheme(.light)
}
#Preview("ContinueCarousel · Dark") {
    ContinueCarousel(entries: [IosBridge.shared.sampleHistory(title: "Lâmina do Norte")]).preferredColorScheme(.dark)
}
#Preview("NewChaptersSection · Light") {
    NewChaptersSection(items: Array(IosBridge.shared.sampleLibrary().prefix(2))).padding().preferredColorScheme(.light)
}
#Preview("NewChaptersSection · Dark") {
    NewChaptersSection(items: Array(IosBridge.shared.sampleLibrary().prefix(2))).padding().preferredColorScheme(.dark)
}
#Preview("CharacterButtons · Light") {
    CharacterButtons(names: ["Batman", "Homem-Aranha", "X-Men"], counts: [12, 9, 6], selected: "Batman").preferredColorScheme(.light)
}
#Preview("CharacterButtons · Dark") {
    CharacterButtons(names: ["Batman", "Homem-Aranha", "X-Men"], counts: [12, 9, 6]).preferredColorScheme(.dark)
}
#Preview("CharactersSheet · Light") { CharactersSheet(names: ["Batman", "Superman"]).preferredColorScheme(.light) }
#Preview("CharactersSheet · Dark") { CharactersSheet(names: ["Batman", "Superman"]).preferredColorScheme(.dark) }
#Preview("StateCorner · Light") { StateCornerPreview().preferredColorScheme(.light) }
#Preview("StateCorner · Dark") { StateCornerPreview().preferredColorScheme(.dark) }
#Preview("CornerPill · Light") { CornerPill(text: "+3", background: TrackingColors.new, content: TrackingColors.ink).preferredColorScheme(.light) }
#Preview("CornerPill · Dark") { CornerPill(text: "2×", background: Palette.primary, content: Palette.onPrimary).preferredColorScheme(.dark) }

private struct StateCornerPreview: View {
    var body: some View {
        HStack(spacing: 8) {
            ForEach(IosBridge.shared.sampleLibrary(), id: \.manga.source.title) { item in
                ZStack(alignment: .bottomTrailing) {
                    Color(red: 0.17, green: 0.23, blue: 0.29)
                    StateCorner(item: item)
                }
                .frame(width: 70, height: 105)
            }
        }
        .padding()
    }
}
