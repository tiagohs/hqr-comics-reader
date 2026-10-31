import Shared
import SwiftUI

/// SCR-DL-001: the queue and the downloaded chapters, with pause, order and the download options (RF-62 to RF-68).
struct DownloadsScreen: View {
    @EnvironmentObject private var navigator: Navigator
    @StateObject private var model = ScreenModel(Core.graph.downloadsViewModel()) {
        Screen<DownloadsViewModel.State>(viewModel: $0, flow: $0.state)
    }
    @State private var confirmDelete = false

    var body: some View {
        let state = model.state
        List {
            if state.queue.contains(where: \.lowSpace) {
                ErrorBox(message: L("downloads_low_space"), action: L("downloads_resume_all")) { model.viewModel.resumeAll() }
                    .listRowBackground(Color.clear)
            }
            if !state.queue.isEmpty {
                Section(L("downloads_queue")) {
                    ForEach(Array(state.queue.enumerated()), id: \.element.chapterId) { index, item in
                        queueRow(item, first: index == 0, last: index == state.queue.count - 1)
                    }
                }
            }
            if !state.done.isEmpty {
                Section(L("downloads_done")) {
                    ForEach(state.done, id: \.chapterId) { item in
                        VStack(alignment: .leading) {
                            Text(item.mangaTitle).lineLimit(1)
                            Text(item.chapterLabel).font(.caption).foregroundStyle(Palette.onSurfaceVariant)
                        }
                        .swipeActions {
                            Button(role: .destructive) { model.viewModel.remove(chapterId: item.chapterId) } label: {
                                Label(L("manga_delete_download"), systemImage: "trash")
                            }
                        }
                        .contextMenu {
                            Button(role: .destructive) { model.viewModel.removeWork(mangaId: item.mangaId) } label: {
                                Label(L("downloads_delete_work", item.mangaTitle), systemImage: "trash")
                            }
                        }
                    }
                }
            }
            if state.queue.isEmpty && state.done.isEmpty && !state.loading {
                EmptyState(title: L("downloads_empty_title"), message: L("downloads_empty_message"), systemImage: "arrow.down.circle")
                    .listRowBackground(Color.clear)
            }
            Section {
                Toggle(isOn: Binding(get: { model.state.wifiOnly }, set: { model.viewModel.setWifiOnly(on: $0) })) {
                    Text(L("downloads_wifi_only"))
                    Text(L("downloads_wifi_only_hint"))
                }
                Toggle(isOn: Binding(get: { model.state.deleteAfterRead }, set: { model.viewModel.setDeleteAfterRead(on: $0) })) {
                    Text(L("downloads_delete_after_read"))
                    Text(L("downloads_delete_after_read_hint"))
                }
                Toggle(isOn: Binding(get: { model.state.saveAsCbz }, set: { model.viewModel.setSaveAsCbz(on: $0) })) {
                    Text(L("downloads_cbz"))
                    Text(L("downloads_cbz_hint"))
                }
            } footer: {
                VStack(alignment: .leading, spacing: 4) {
                    Text(L("downloads_used", ByteCountFormatter.string(fromByteCount: state.usedBytes, countStyle: .file)))
                    Text(L("downloads_ios_background"))
                }
            }
        }
        .navigationTitle(L("downloads_title"))
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Menu {
                    Button(L("downloads_pause_all")) { model.viewModel.pauseAll() }
                    Button(L("downloads_resume_all")) { model.viewModel.resumeAll() }
                    Button(L("downloads_delete_all"), role: .destructive) { confirmDelete = true }
                } label: { Image(systemName: "ellipsis.circle").accessibilityLabel(L("action_more")) }
            }
        }
        .confirmationDialog(L("downloads_delete_all_confirm"), isPresented: $confirmDelete, titleVisibility: .visible) {
            Button(L("downloads_delete_all"), role: .destructive) { model.viewModel.removeAll() }
        }
    }

    private func queueRow(_ item: DownloadItem, first: Bool, last: Bool) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                VStack(alignment: .leading) {
                    Text(item.mangaTitle).lineLimit(1)
                    Text(item.chapterLabel).font(.caption).foregroundStyle(Palette.onSurfaceVariant)
                }
                Spacer()
                Text(stateText(item)).font(.caption).foregroundStyle(item.state == .error ? Palette.error : Palette.onSurfaceVariant)
            }
            if item.pagesTotal > 0 {
                ProgressView(value: Double(item.pagesDone), total: Double(item.pagesTotal))
            }
        }
        .swipeActions {
            Button(role: .destructive) { model.viewModel.remove(chapterId: item.chapterId) } label: { Image(systemName: "xmark") }
        }
        .contextMenu {
            if !first { Button(L("action_move_up", item.chapterLabel)) { model.viewModel.move(chapterId: item.chapterId, by: -1) } }
            if !last { Button(L("action_move_down", item.chapterLabel)) { model.viewModel.move(chapterId: item.chapterId, by: 1) } }
        }
    }

    private func stateText(_ item: DownloadItem) -> String {
        switch item.state {
        case .running: "\(item.pagesDone)/\(item.pagesTotal)"
        case .paused: "⏸"
        case .waitingWifi: L("downloads_wifi_only")
        case .error: item.error ?? L("error_unknown")
        default: ""
        }
    }
}

#Preview("DownloadsScreen · Light") { DownloadsScreen().previewScreen().preferredColorScheme(.light) }
#Preview("DownloadsScreen · Dark") { DownloadsScreen().previewScreen().preferredColorScheme(.dark) }
