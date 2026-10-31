import Shared
import SwiftUI

/// SCR-HIS-001: what was read, by day, with remove and clear (RF-59 to RF-61).
struct HistoryScreen: View {
    @EnvironmentObject private var navigator: Navigator
    @StateObject private var model = ScreenModel(Core.graph.historyViewModel()) {
        Screen<HistoryViewModel.State>(viewModel: $0, flow: $0.state)
    }
    @State private var confirmClear = false
    @State private var removed: HistoryEntry?

    var body: some View {
        let state = model.state
        Group {
            if !state.loading && state.days.isEmpty {
                EmptyState(title: L("history_empty_title"), message: L("history_empty_message"), systemImage: "clock")
            } else {
                List {
                    ForEach(state.days, id: \.date) { day in
                        Section(title(day)) {
                            ForEach(day.entries, id: \.chapter.id) { entry in row(entry) }
                        }
                    }
                }
                .listStyle(.insetGrouped)
                .screenBackground()
            }
        }
        .navigationTitle(L("nav_history"))
        .toolbar {
            if !state.days.isEmpty {
                Button(role: .destructive) { confirmClear = true } label: {
                    Image(systemName: "trash").accessibilityLabel(L("history_clear"))
                }
            }
        }
        .confirmationDialog(L("history_clear_confirm"), isPresented: $confirmClear, titleVisibility: .visible) {
            Button(L("history_clear"), role: .destructive) { model.viewModel.clearAll() }
        }
        .safeAreaInset(edge: .bottom) {
            if let entry = removed {
                UndoBar(text: L("history_removed")) {
                    model.viewModel.undoRemove(entry: entry)
                    removed = nil
                } dismiss: { removed = nil }
            }
        }
    }

    private func title(_ day: HistoryViewModel.Day) -> String {
        if day.isToday { return L("history_today") }
        if day.isYesterday { return L("history_yesterday") }
        return day.date.description
    }

    private func row(_ entry: HistoryEntry) -> some View {
        Button {
            navigator.read(mangaId: entry.manga.id, chapterId: entry.chapter.id,
                           page: entry.chapter.read ? 0 : entry.chapter.lastPage)
        } label: {
            HStack(spacing: 12) {
                CoverImage(manga: entry.manga.source, width: 44)
                VStack(alignment: .leading, spacing: 4) {
                    Text(entry.manga.source.title).font(.headline).lineLimit(1).foregroundStyle(Palette.onSurface)
                    Text(entry.chapter.label).font(.subheadline).foregroundStyle(Palette.onSurfaceVariant).lineLimit(1)
                    Text(Date(timeIntervalSince1970: Double(entry.readAtEpochMs) / 1000).formatted(date: .omitted, time: .shortened))
                        .font(.caption).foregroundStyle(Palette.onSurfaceVariant)
                }
            }
        }
        .swipeActions {
            Button(role: .destructive) {
                model.viewModel.remove(entry: entry)
                removed = entry
            } label: { Label(L("history_remove"), systemImage: "trash") }
        }
        .contextMenu {
            Button { navigator.open(.manga(entry.manga.id)) } label: { Label(L("manga_details"), systemImage: "book") }
        }
    }
}

#Preview("HistoryScreen · Light") { HistoryScreen().previewScreen().preferredColorScheme(.light) }
#Preview("HistoryScreen · Dark") { HistoryScreen().previewScreen().preferredColorScheme(.dark) }
