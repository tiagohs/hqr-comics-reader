import Combine
import Shared
import SwiftUI

/// SCR-EXP-002: a source's popular, latest and search lists, page by page (RF-21), its filters (RF-22), saved
/// searches (RF-24) and feed (RF-25), with the Cloudflare verification when the source asks for it (RF-106).
struct BrowseSourceScreen: View {
    let sourceId: String
    @EnvironmentObject private var navigator: Navigator
    @StateObject private var models: BrowseModels
    @State private var query: String
    @State private var verified = false
    @StateObject private var favorites = Favorites()
    @State private var showFilters = false
    /// The saved search being named: a new one (nil inside) or one being renamed.
    @State private var naming: SavedSearch??
    @State private var name = ""
    @State private var notice: String?

    private var model: ScreenModel<BrowseSourceViewModel, BrowseSourceViewModel.State> { models.screen }
    private var savedSearches: [SavedSearch] { (models.saved.value as? [SavedSearch]) ?? [] }

    init(sourceId: String, query: String) {
        self.sourceId = sourceId
        _query = State(initialValue: query)
        // One ViewModel per screen, made once: the autoclosure runs only when SwiftUI first creates the state.
        _models = StateObject(wrappedValue: BrowseModels(sourceId: sourceId, query: query))
    }

    var body: some View {
        let state = model.state
        ScrollView {
            VStack(spacing: 12) {
                if state.listing != .search && state.hasLatest {
                    Picker("", selection: Binding(get: { state.listing }, set: { model.viewModel.show(listing: $0) })) {
                        Text(L("browse_popular")).tag(BrowseSourceViewModel.Listing.popular)
                        Text(L("browse_latest")).tag(BrowseSourceViewModel.Listing.latest)
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal, Metrics.spaceMarginCompact)
                }
                if !savedSearches.isEmpty { savedRow }
                content(state)
            }
            .padding(.vertical, 8)
        }
        .screenBackground()
        .refreshable { model.viewModel.refresh() }
        .navigationTitle(sourceDisplayName(sourceId))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            // RF-115: the star puts this source's latest in the feed.
            ToolbarItemGroup(placement: .primaryAction) {
                FavoriteStar(name: sourceDisplayName(sourceId), on: favorites.ids.contains(sourceId)) { favorites.toggle(sourceId) }
                // RF-22: the source's filters, with how many are on.
                if !state.filters.isEmpty {
                    Button { showFilters = true } label: {
                        Image(systemName: state.activeFilters > 0 ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle")
                    }
                    .accessibilityLabel(state.activeFilters > 0 ? L("filter_active", Int(state.activeFilters)) : L("filter_title"))
                }
                Menu {
                    // RF-24: what is on screen, kept by name.
                    if state.listing == .search && state.canSearch {
                        Button { startNaming(nil, state.query) } label: { Label(L("saved_save"), systemImage: "bookmark") }
                    }
                    // RF-25: this source's latest in the feed.
                    Button { addToFeed(nil) } label: { Label(L("feed_add_latest"), systemImage: "newspaper") }
                } label: {
                    Image(systemName: "ellipsis.circle").accessibilityLabel(L("action_more"))
                }
            }
        }
        .sheet(isPresented: $showFilters) {
            SourceFiltersSheet(filters: state.filters, defaults: model.viewModel.defaultFilters()) { filters in
                model.viewModel.applyFilters(filters: filters)
            }
        }
        .alert(L("saved_title"), isPresented: Binding(get: { naming != nil }, set: { if !$0 { naming = nil } })) {
            TextField(L("saved_name"), text: $name)
            Button(L("action_cancel"), role: .cancel) { naming = nil }
            Button(L("action_save")) { saveName() }
        }
        .alert(notice ?? "", isPresented: Binding(get: { notice != nil }, set: { if !$0 { notice = nil } })) {
            Button(L("action_ok"), role: .cancel) {}
        }
        .alert(L("feed_full_favorite"), isPresented: $favorites.feedFull) { Button(L("action_ok"), role: .cancel) {} }
        .modifier(SearchIf(enabled: state.canSearch, query: $query, prompt: L("search_hint")) {
            model.viewModel.search(query: query, filters: state.filters)
        })
        .onAppear {
            // Back from the verification page: one retry with the new cookies (RF-106).
            if verified {
                verified = false
                model.viewModel.retry()
            }
        }
    }

    @ViewBuilder
    private func content(_ state: BrowseSourceViewModel.State) -> some View {
        if let error = state.error, state.items.isEmpty {
            if error == .challenge, let url = state.webUrl {
                ErrorBox(message: L("challenge_hint"), action: L("challenge_verify")) {
                    verified = true
                    navigator.open(.challenge(url: url))
                }
                .padding(.horizontal, Metrics.spaceMarginCompact)
            } else {
                ErrorBox(message: sourceErrorMessage(error, source: sourceDisplayName(sourceId)), action: L("action_retry"),
                         onAction: { model.viewModel.retry() },
                         secondary: state.webUrl.flatMap(URL.init(string:)).map { url in (L("action_open_browser"), { UIApplication.shared.open(url) }) })
                    .padding(.horizontal, Metrics.spaceMarginCompact)
            }
        } else if state.loading && state.items.isEmpty {
            // Soft covers in their final shape and where it is looking (ux-foundations §8).
            VStack(spacing: 12) {
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 104), spacing: Metrics.spaceCoverGap)], spacing: 12) {
                    ForEach(0..<9, id: \.self) { _ in Skeleton().aspectRatio(2.0 / 3.0, contentMode: .fit) }
                }
                HStack(spacing: 8) {
                    DotsLoader()
                    Text(L("manga_searching_source", sourceDisplayName(sourceId))).font(.caption.weight(.semibold))
                        .foregroundStyle(Palette.onSurfaceVariant)
                }
            }
            .padding(.horizontal, Metrics.spaceMarginCompact)
        } else if state.items.isEmpty {
            EmptyState(title: L("browse_no_results"), message: L("browse_no_results_message"), systemImage: "magnifyingglass")
                .frame(minHeight: 320)
        } else {
            CoverGrid(items: state.items) { manga in
                Button { open(manga) } label: {
                    CoverCard(manga: manga, inLibrary: state.inLibrary.contains(manga.key))
                }
                .buttonStyle(.plain)
                .contextMenu {
                    Button { toggle(manga) } label: {
                        Label(state.inLibrary.contains(manga.key) ? L("manga_remove_library") : L("manga_add_library"),
                              systemImage: state.inLibrary.contains(manga.key) ? "minus.circle" : "plus.circle")
                    }
                }
                .onAppear { if manga == state.items.last { model.viewModel.loadMore() } }
            }
            footer(state)
        }
    }

    @ViewBuilder
    private func footer(_ state: BrowseSourceViewModel.State) -> some View {
        if state.loadingMore {
            ProgressView().padding()
        } else if let error = state.loadMoreError {
            ErrorBox(message: error.message, action: L("action_retry")) { model.viewModel.loadMore() }
                .padding(.horizontal, Metrics.spaceMarginCompact)
        }
    }

    private func open(_ manga: SourceManga) {
        launch {
            let id = try await model.viewModel.open(manga: manga)
            navigator.openWork(id.int64Value, from: manga)
        }
    }

    // MARK: Saved searches (RF-24)

    private var savedRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(savedSearches, id: \.id) { search in
                    Menu {
                        Button { addToFeed(search) } label: { Label(L("feed_add"), systemImage: "newspaper") }
                        Button { startNaming(search, search.name) } label: { Label(L("saved_rename"), systemImage: "pencil") }
                        Button(role: .destructive) { model.viewModel.deleteSaved(search: search) } label: {
                            Label(L("saved_delete"), systemImage: "trash")
                        }
                    } label: {
                        Label(search.name, systemImage: "bookmark")
                            .font(.subheadline)
                            .padding(.horizontal, 12).padding(.vertical, 6)
                            .overlay(Capsule().stroke(Palette.outline, lineWidth: 1))
                    } primaryAction: {
                        query = search.query
                        model.viewModel.applySaved(search: search)
                    }
                    .accessibilityHint(L("saved_options", search.name))
                }
            }
            .padding(.horizontal, Metrics.spaceMarginCompact)
        }
    }

    private func startNaming(_ search: SavedSearch?, _ text: String) {
        name = text
        naming = .some(search)
    }

    private func saveName() {
        let text = name.trimmingCharacters(in: .whitespaces)
        defer { naming = nil }
        guard !text.isEmpty, let target = naming else { return }
        if let search = target {
            model.viewModel.renameSaved(search: search, name: text)
        } else {
            model.viewModel.saveSearch(name: text)
        }
    }

    private func addToFeed(_ search: SavedSearch?) {
        launch {
            let added = try await model.viewModel.addToFeed(search: search)
            notice = added.boolValue ? L("feed_added") : L("feed_full")
        }
    }

    private func toggle(_ manga: SourceManga) {
        launch { _ = try await model.viewModel.toggleLibrary(manga: manga) }
    }
}

/// The source's ViewModel with its saved searches (RF-24).
@MainActor
final class BrowseModels: ObservableObject {
    let screen: ScreenModel<BrowseSourceViewModel, BrowseSourceViewModel.State>
    let saved: FlowModel<NSArray>
    private var changes: [AnyCancellable] = []

    init(sourceId: String, query: String) {
        let viewModel = Core.graph.browseSourceViewModel(sourceId: sourceId, query: query)
        screen = ScreenModel(viewModel) { Screen<BrowseSourceViewModel.State>(viewModel: $0, flow: $0.state) }
        saved = FlowModel(viewModel.savedSearches)
        changes = [
            screen.objectWillChange.sink { [weak self] _ in self?.objectWillChange.send() },
            saved.objectWillChange.sink { [weak self] _ in self?.objectWillChange.send() },
        ]
    }
}

/// `.searchable` only where the source can search (RF-22).
struct SearchIf: ViewModifier {
    let enabled: Bool
    @Binding var query: String
    let prompt: String
    let submit: () -> Void

    func body(content: Content) -> some View {
        if enabled {
            content.searchable(text: $query, prompt: prompt).onSubmit(of: .search, submit)
        } else {
            content
        }
    }
}

#Preview("BrowseSourceScreen · Light") { BrowseSourceScreen(sourceId: "local", query: "").previewScreen().preferredColorScheme(.light) }
#Preview("BrowseSourceScreen · Dark") { BrowseSourceScreen(sourceId: "local", query: "").previewScreen().preferredColorScheme(.dark) }
