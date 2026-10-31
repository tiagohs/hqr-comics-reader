import Shared
import SwiftUI

/// SCR-OBR-001: cover, details, the main read button, chapters with read and download marks, and the cover's color as
/// the tint (RF-29 to RF-35, RF-62, RF-98).
struct MangaScreen: View {
    let mangaId: Int64
    @EnvironmentObject private var navigator: Navigator
    @Environment(\.colorScheme) private var scheme
    @StateObject private var model: ScreenModel<MangaViewModel, MangaViewModel.State>
    @StateObject private var downloads: FlowModel<NSDictionary>
    @StateObject private var selection: FlowModel<NSSet>
    @State private var expanded = false

    init(mangaId: Int64) {
        self.mangaId = mangaId
        let viewModel = Core.graph.mangaViewModel(mangaId: mangaId)
        _model = StateObject(wrappedValue: ScreenModel(viewModel) { Screen<MangaViewModel.State>(viewModel: $0, flow: $0.state) })
        _downloads = StateObject(wrappedValue: FlowModel(viewModel.downloadStates))
        _selection = StateObject(wrappedValue: FlowModel(viewModel.selected))
    }

    private var selected: Set<Int64> {
        Set(selection.value.compactMap { ($0 as? KotlinLong)?.int64Value })
    }

    var body: some View {
        let state = model.state
        List {
            if let manga = state.manga {
                Section { header(manga, state: state) }
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
            }
            if let error = state.error, state.chapters.isEmpty {
                ErrorBox(message: error.message, action: L("action_retry")) { model.viewModel.refresh() }
                    .listRowBackground(Color.clear)
            }
            Section {
                ForEach(state.chapters, id: \.id) { chapter in row(chapter, state: state) }
            } header: {
                HStack {
                    Text(L("manga_chapters", Int(state.totalChapters)))
                    Spacer()
                    filterMenu(state)
                }
            }
            if state.chapters.isEmpty && !state.refreshing && state.error == nil {
                Text(L("manga_no_chapters")).foregroundStyle(Palette.onSurfaceVariant)
            }
        }
        .listStyle(.plain)
        .screenBackground()
        .refreshable { model.viewModel.refresh() }
        .navigationTitle(state.manga?.source.title ?? "")
        .navigationBarTitleDisplayMode(.inline)
        .tint(tint(state.manga))
        .toolbar { toolbar(state) }
        .overlay {
            if state.manga == nil && state.refreshing { ProgressView() }
        }
    }

    // MARK: Header

    private func header(_ manga: Manga, state: MangaViewModel.State) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 16) {
                CoverImage(manga: manga.source, width: 112)
                VStack(alignment: .leading, spacing: 6) {
                    Text(manga.source.title).font(.title3.weight(.bold)).lineLimit(3)
                    if !manga.source.authors.isEmpty {
                        Text(manga.source.authors.joined(separator: ", ")).font(.subheadline)
                            .foregroundStyle(Palette.onSurfaceVariant)
                    }
                    Text(sourceDisplayName(manga.source.sourceKey)).font(.caption).foregroundStyle(Palette.onSurfaceVariant)
                }
            }
            HStack(spacing: 12) {
                readButton(state)
                Button {
                    model.viewModel.toggleLibrary()
                } label: {
                    Label(manga.inLibrary ? L("manga_in_library") : L("manga_add_library"),
                          systemImage: manga.inLibrary ? "heart.fill" : "heart")
                }
                .buttonStyle(.bordered)
            }
            if let description = manga.source.description_, !description.isEmpty {
                Text(description)
                    .font(.subheadline)
                    .lineLimit(expanded ? nil : 3)
                    .onTapGesture { withAnimation { expanded.toggle() } }
                    .accessibilityHint(expanded ? L("action_less_text") : L("action_more_text"))
            }
            if !manga.source.genres.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack { ForEach(manga.source.genres, id: \.self) { Chip(text: $0) } }
                }
            }
        }
        .padding(Metrics.spaceMarginCompact)
    }

    @ViewBuilder
    private func readButton(_ state: MangaViewModel.State) -> some View {
        let bridge = IosBridge.shared
        if let chapter = bridge.actionChapter(action: state.action) {
            let kind = bridge.actionKind(action: state.action)
            let page = bridge.actionPage(action: state.action)
            Button {
                navigator.read(mangaId: mangaId, chapterId: chapter.id, page: page)
            } label: {
                Group {
                    switch kind {
                    case "continue": Text(L("manga_continue_chapter", chapter.numberText, Int(page) + 1))
                    case "again": Text(L("action_read_again"))
                    default: Text(L("action_read"))
                    }
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
        }
    }

    // MARK: Chapters

    private func row(_ chapter: Chapter, state: MangaViewModel.State) -> some View {
        let download = downloads.value[KotlinLong(value: chapter.id)] as? DownloadItem
        let isSelected = selected.contains(chapter.id)
        return Button {
            if !selected.isEmpty {
                model.viewModel.toggleSelected(id: chapter.id)
            } else if let external = chapter.source.externalUrl, let url = URL(string: external) {
                UIApplication.shared.open(url)
            } else {
                navigator.read(mangaId: mangaId, chapterId: chapter.id, page: chapter.read ? 0 : chapter.lastPage)
            }
        } label: {
            HStack(spacing: 12) {
                if !selected.isEmpty {
                    Image(systemName: isSelected ? "checkmark.circle.fill" : "circle").foregroundStyle(.tint)
                }
                VStack(alignment: .leading, spacing: 3) {
                    HStack(spacing: 6) {
                        Text(chapter.label).font(.body.weight(chapter.read ? .regular : .semibold)).lineLimit(1)
                        if state.newChapterIds.contains(KotlinLong(value: chapter.id)) { Chip(text: L("manga_new")) }
                    }
                    .foregroundStyle(chapter.read ? Palette.onSurfaceVariant : Palette.onSurface)
                    subtitle(chapter).font(.caption).foregroundStyle(Palette.onSurfaceVariant)
                }
                Spacer()
                downloadMark(chapter, item: download)
            }
        }
        .buttonStyle(.plain)
        .contextMenu { menu(chapter, download: download) }
        .swipeActions(edge: .leading) {
            Button { IosBridge.shared.setRead(viewModel: model.viewModel, chapterIds: [KotlinLong(value: chapter.id)], read: !chapter.read) } label: {
                Image(systemName: chapter.read ? "eye.slash" : "eye")
            }
        }
    }

    private func subtitle(_ chapter: Chapter) -> Text {
        var parts: [String] = []
        if let group = chapter.source.group { parts.append(group) }
        if let date = chapter.source.uploadedAtEpochMs?.int64Value {
            parts.append(Date(timeIntervalSince1970: Double(date) / 1000).formatted(date: .abbreviated, time: .omitted))
        }
        if !chapter.read && chapter.lastPage > 0, let count = chapter.pageCount?.intValue {
            parts.append(L("manga_page_of", Int(chapter.lastPage) + 1, count))
        }
        if chapter.source.externalUrl != nil { parts.append(L("manga_external")) }
        return Text(parts.joined(separator: " · "))
    }

    @ViewBuilder
    private func downloadMark(_ chapter: Chapter, item: DownloadItem?) -> some View {
        if chapter.downloaded {
            Image(systemName: "checkmark.circle.fill").foregroundStyle(.tint).accessibilityLabel(L("manga_downloaded"))
        } else if let item {
            if item.pagesTotal > 0 {
                ProgressView(value: Double(item.pagesDone), total: Double(item.pagesTotal)).progressViewStyle(.circular)
                    .frame(width: 22, height: 22)
            } else {
                Image(systemName: "clock").foregroundStyle(Palette.onSurfaceVariant)
            }
        } else if model.viewModel.canDownload && chapter.source.externalUrl == nil {
            Button {
                IosBridge.shared.download(viewModel: model.viewModel, chapterIds: [KotlinLong(value: chapter.id)])
            } label: {
                Image(systemName: "arrow.down.circle").accessibilityLabel(L("manga_download"))
            }
            .buttonStyle(.borderless)
        }
    }

    @ViewBuilder
    private func menu(_ chapter: Chapter, download: DownloadItem?) -> some View {
        let ids = [KotlinLong(value: chapter.id)]
        Button { IosBridge.shared.setRead(viewModel: model.viewModel, chapterIds: ids, read: !chapter.read) } label: {
            Label(chapter.read ? L("manga_mark_unread") : L("manga_mark_read"), systemImage: chapter.read ? "eye.slash" : "eye")
        }
        Button { model.viewModel.markPreviousAsRead(chapter: chapter) } label: {
            Label(L("manga_mark_previous"), systemImage: "arrow.up.to.line")
        }
        Button { model.viewModel.toggleSelected(id: chapter.id) } label: {
            Label(L("manga_select"), systemImage: "checkmark.circle")
        }
        if chapter.downloaded || download != nil {
            Button(role: .destructive) { IosBridge.shared.removeDownloads(viewModel: model.viewModel, chapterIds: ids) } label: {
                Label(L("manga_delete_download"), systemImage: "trash")
            }
        }
    }

    private func filterMenu(_ state: MangaViewModel.State) -> some View {
        Menu {
            Picker(L("manga_order"), selection: Binding(get: { state.order }, set: { model.viewModel.setOrder(value: $0) })) {
                Text("↓").tag(MangaViewModel.Order.newestFirst)
                Text("↑").tag(MangaViewModel.Order.oldestFirst)
            }
            Picker("", selection: Binding(get: { state.filter }, set: { model.viewModel.setFilter(value: $0) })) {
                Text(L("manga_filter_all")).tag(MangaViewModel.ChapterFilter.all)
                Text(L("manga_filter_unread")).tag(MangaViewModel.ChapterFilter.unread)
                Text(L("manga_downloaded")).tag(MangaViewModel.ChapterFilter.downloaded)
            }
            if state.groups.count > 1 {
                Picker(L("manga_open_group"), selection: Binding(get: { state.group ?? "" },
                                                                set: { model.viewModel.setGroup(value: $0.isEmpty ? nil : $0) })) {
                    Text(L("manga_filter_all")).tag("")
                    ForEach(state.groups, id: \.self) { Text($0).tag($0) }
                }
            }
        } label: {
            Image(systemName: "line.3.horizontal.decrease.circle").accessibilityLabel(L("manga_order"))
        }
    }

    @ToolbarContentBuilder
    private func toolbar(_ state: MangaViewModel.State) -> some ToolbarContent {
        if !selected.isEmpty {
            ToolbarItemGroup(placement: .bottomBar) {
                let ids = selected.map { KotlinLong(value: $0) }
                Button(L("manga_mark_read")) { IosBridge.shared.setRead(viewModel: model.viewModel, chapterIds: ids, read: true) }
                Spacer()
                if model.viewModel.canDownload {
                    Button(L("manga_download")) { IosBridge.shared.download(viewModel: model.viewModel, chapterIds: ids) }
                    Spacer()
                }
                Button(L("manga_clear_selection")) { model.viewModel.clearSelection() }
            }
        }
        ToolbarItem(placement: .primaryAction) {
            Menu {
                if model.viewModel.canDownload {
                    Button { model.viewModel.downloadUnread(count: KotlinInt(value: 1)) } label: {
                        Label(L("manga_download_next"), systemImage: "arrow.down")
                    }
                    Button { model.viewModel.downloadUnread(count: nil) } label: {
                        Label(L("manga_download_unread"), systemImage: "arrow.down.circle")
                    }
                }
                Button { navigator.open(.migrate(mangaId)) } label: {
                    Label(L("migrate_action"), systemImage: "arrow.left.arrow.right")
                }
                if let link = state.manga?.source.url, let url = URL(string: link) {
                    Link(destination: url) { Label(L("manga_open_site"), systemImage: "safari") }
                    ShareLink(item: url)
                }
            } label: {
                Image(systemName: "ellipsis.circle").accessibilityLabel(L("action_more"))
            }
        }
    }

    // MARK: Cover color

    /// RF-98: the work takes the cover's primary color; without one the app's violet stays.
    private func tint(_ manga: Manga?) -> Color {
        let enabled = (Core.graph.preferences().coverColor.value as? KotlinBoolean)?.boolValue ?? true
        guard enabled, let seed = manga?.coverColor?.int32Value else {
            return Palette.primary
        }
        let roles = CoverColor.shared.roles(seed: seed, dark: scheme == .dark)
        return Color(argb: roles.primary)
    }
}

extension Color {
    /// An ARGB int from the core (cover colors).
    init(argb: Int32) {
        let value = UInt32(bitPattern: argb)
        self.init(.sRGB,
                  red: Double((value >> 16) & 0xFF) / 255,
                  green: Double((value >> 8) & 0xFF) / 255,
                  blue: Double(value & 0xFF) / 255,
                  opacity: Double((value >> 24) & 0xFF) / 255)
    }
}

#Preview("MangaScreen · Light") { MangaScreen(mangaId: 1).previewScreen().preferredColorScheme(.light) }
#Preview("MangaScreen · Dark") { MangaScreen(mangaId: 1).previewScreen().preferredColorScheme(.dark) }
