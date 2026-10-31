import Shared
import SwiftUI

/// SCR-EXP-002: a source's popular, latest and search lists, page by page (RF-21, RF-22), with the Cloudflare
/// verification when the source asks for it (RF-106).
struct BrowseSourceScreen: View {
    let sourceId: String
    @EnvironmentObject private var navigator: Navigator
    @StateObject private var model: ScreenModel<BrowseSourceViewModel, BrowseSourceViewModel.State>
    @State private var query: String
    @State private var verified = false

    init(sourceId: String, query: String) {
        self.sourceId = sourceId
        _query = State(initialValue: query)
        _model = StateObject(wrappedValue: ScreenModel(Core.graph.browseSourceViewModel(sourceId: sourceId, query: query)) {
            Screen<BrowseSourceViewModel.State>(viewModel: $0, flow: $0.state)
        })
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
                content(state)
            }
            .padding(.vertical, 8)
        }
        .screenBackground()
        .refreshable { model.viewModel.refresh() }
        .navigationTitle(sourceDisplayName(sourceId))
        .navigationBarTitleDisplayMode(.inline)
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
                ErrorState(kind: error) { model.viewModel.retry() }.frame(minHeight: 320)
            }
        } else if state.loading && state.items.isEmpty {
            ProgressView().frame(maxWidth: .infinity, minHeight: 320)
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
            navigator.open(.manga(id.int64Value))
        }
    }

    private func toggle(_ manga: SourceManga) {
        launch { _ = try await model.viewModel.toggleLibrary(manga: manga) }
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
