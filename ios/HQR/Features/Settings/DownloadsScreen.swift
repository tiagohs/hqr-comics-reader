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
            // RF-66, RF-80: what the app takes on the device and the way to free the cache.
            Section {
                StoragePanel(downloadsBytes: state.usedBytes, chapters: state.done.count)
            }
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 12, trailing: 20))
            if !state.done.isEmpty {
                Section(L("downloads_done")) {
                    ForEach(state.done, id: \.chapterId) { item in
                        VStack(alignment: .leading) {
                            Text(item.mangaTitle).lineLimit(1)
                            Text(item.chapterLabel).font(.caption).foregroundStyle(Palette.onSurfaceVariant)
                        }
                        .swipeActions {
                            Button(role: .destructive) { model.viewModel.remove(chapterId: item.chapterId) } label: {
                                Label(L("download_delete", item.chapterLabel), systemImage: "trash")
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
            Button(role: .destructive) { model.viewModel.remove(chapterId: item.chapterId) } label: {
                Label(L("download_cancel", item.chapterLabel), systemImage: "xmark")
            }
        }
        .contextMenu {
            if !first { Button(L("action_move_up", item.chapterLabel)) { model.viewModel.move(chapterId: item.chapterId, by: -1) } }
            if !last { Button(L("action_move_down", item.chapterLabel)) { model.viewModel.move(chapterId: item.chapterId, by: 1) } }
        }
    }

    private func stateText(_ item: DownloadItem) -> String {
        switch item.state {
        case .queued: L("download_state_queued")
        case .running: L("download_state_running", Int(item.pagesDone), Int(item.pagesTotal))
        case .paused: item.lowSpace ? L("download_state_low_space") : L("download_state_paused")
        // Waiting for Wi-Fi under "só no Wi-Fi" (RF-64), or for any network after losing it.
        case .waitingWifi: model.state.wifiOnly ? L("download_state_wifi") : L("download_state_network")
        case .error: L("download_state_error", item.error ?? L("error_unknown"))
        default: ""
        }
    }
}

/// The space of the downloads and of the image cache next to what is free, as one bar with a legend (RF-66, RF-80).
struct StoragePanel: View {
    let downloadsBytes: Int64
    let chapters: Int
    @State private var cacheBytes: Int64 = 0
    private let free = Core.graph.freeBytes()

    var body: some View {
        let total = max(1, downloadsBytes + cacheBytes + max(0, free))
        VStack(alignment: .leading, spacing: 10) {
            Text(L("storage_title")).font(ComicFont.bangers(22, relativeTo: .headline))
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .lastTextBaseline, spacing: 8) {
                    Text(bytes(downloadsBytes + cacheBytes)).font(ComicFont.bangers(34))
                    if free >= 0 {
                        Text(L("storage_used", bytes(free))).font(.caption.weight(.semibold)).foregroundStyle(Palette.onSurfaceVariant)
                    }
                }
                GeometryReader { geo in
                    HStack(spacing: 0) {
                        Color.accentColor.frame(width: geo.size.width * CGFloat(downloadsBytes) / CGFloat(total))
                        Palette.onSurface.frame(width: geo.size.width * CGFloat(cacheBytes) / CGFloat(total))
                        Spacer(minLength: 0)
                    }
                }
                .frame(height: 20)
                .overlay(Rectangle().stroke(Palette.onSurface, lineWidth: 2))
                .accessibilityHidden(true)
                legend(Color.accentColor, L("storage_downloads", chapters), bytes(downloadsBytes))
                legend(Palette.onSurface, L("storage_cache"), bytes(cacheBytes))
                Button(L("storage_clear_cache")) {
                    launch {
                        try await Core.graph.imageCache().clear()
                        cacheBytes = (try await Core.graph.imageCache().size()).int64Value
                    }
                }
                .buttonStyle(.bordered)
                .buttonBorderShape(.capsule)
                Text(L("storage_cache_note")).font(.caption).foregroundStyle(Palette.onSurfaceVariant)
            }
            .padding(16)
            .inkPanel()
        }
        .task { cacheBytes = (try? await Core.graph.imageCache().size())?.int64Value ?? 0 }
    }

    private func bytes(_ value: Int64) -> String { ByteCountFormatter.string(fromByteCount: value, countStyle: .file) }

    private func legend(_ color: Color, _ label: String, _ value: String) -> some View {
        HStack(spacing: 8) {
            Rectangle().fill(color).frame(width: 14, height: 14).overlay(Rectangle().stroke(Palette.onSurface, lineWidth: 2))
            Text(label).font(.subheadline)
            Spacer()
            Text(value).font(ComicFont.bangers(17, relativeTo: .body))
        }
        .accessibilityElement(children: .combine)
    }
}

#Preview("StoragePanel · Light") { StoragePanel(downloadsBytes: 1_200_000_000, chapters: 42).padding().previewScreen().preferredColorScheme(.light) }
#Preview("StoragePanel · Dark") { StoragePanel(downloadsBytes: 1_200_000_000, chapters: 42).padding().previewScreen().preferredColorScheme(.dark) }
#Preview("DownloadsScreen · Light") { DownloadsScreen().previewScreen().preferredColorScheme(.light) }
#Preview("DownloadsScreen · Dark") { DownloadsScreen().previewScreen().preferredColorScheme(.dark) }
