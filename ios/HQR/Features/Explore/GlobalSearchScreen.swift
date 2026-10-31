import Shared
import SwiftUI

/// SCR-SRCH-001: one search in every enabled source, grouped by source (RF-23, RF-26 to RF-28).
struct GlobalSearchScreen: View {
    @EnvironmentObject private var navigator: Navigator
    @StateObject private var model = ScreenModel(Core.graph.globalSearchViewModel()) {
        Screen<GlobalSearchViewModel.State>(viewModel: $0, flow: $0.state)
    }
    @State private var query: String

    init(query: String) {
        _query = State(initialValue: query)
    }

    var body: some View {
        let state = model.state
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 20) {
                if !state.searched.isEmpty && state.groups.isEmpty {
                    EmptyState(title: L("search_nothing_for", state.searched), message: L("search_see_sources"),
                               systemImage: "magnifyingglass")
                }
                ForEach(state.groups, id: \.sourceName) { group in section(group, searched: state.searched) }
            }
            .padding(.vertical, 8)
        }
        .screenBackground()
        .navigationTitle(L("action_search"))
        .navigationBarTitleDisplayMode(.inline)
        .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: L("search_hint"))
        .onChange(of: query) { _, text in model.viewModel.type(text: text) }
        .onSubmit(of: .search) { model.viewModel.submit() }
        .onAppear {
            if !query.isEmpty && model.state.searched.isEmpty { model.viewModel.searchAgain(query: query) }
        }
    }

    @ViewBuilder
    private func section(_ group: GlobalSearchViewModel.Group, searched: String) -> some View {
        let id = IosBridge.shared.sourceOfGroup(group: group)
        VStack(alignment: .leading, spacing: 8) {
            Button { navigator.open(.browse(sourceId: id, query: searched)) } label: {
                HStack {
                    Text(sourceDisplayName(id)).font(.headline)
                    Spacer()
                    Image(systemName: "chevron.right").foregroundStyle(Palette.onSurfaceVariant)
                }
            }
            .buttonStyle(.plain)
            .padding(.horizontal, Metrics.spaceMarginCompact)
            if let items = IosBridge.shared.groupResults(group: group) {
                if items.isEmpty {
                    Text(L("search_group_empty")).font(.subheadline).foregroundStyle(Palette.onSurfaceVariant)
                        .padding(.horizontal, Metrics.spaceMarginCompact)
                } else {
                    ScrollView(.horizontal, showsIndicators: false) {
                        LazyHStack(alignment: .top, spacing: Metrics.spaceCoverGap) {
                            ForEach(items, id: \.listId) { manga in
                                Button { open(manga) } label: {
                                    CoverCard(manga: manga, inLibrary: group.inLibrary.contains(manga.key)).frame(width: 112)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, Metrics.spaceMarginCompact)
                    }
                }
            } else if let error = IosBridge.shared.groupError(group: group) {
                ErrorBox(message: sourceErrorMessage(error, source: sourceDisplayName(id)), action: L("action_retry")) {
                    IosBridge.shared.retrySearch(viewModel: model.viewModel, sourceId: id)
                }
                .padding(.horizontal, Metrics.spaceMarginCompact)
            } else {
                HStack(spacing: Metrics.spaceCoverGap) {
                    ForEach(0..<3, id: \.self) { _ in Skeleton().frame(width: 112).aspectRatio(2.0 / 3.0, contentMode: .fit) }
                }
                .padding(.horizontal, Metrics.spaceMarginCompact)
            }
        }
    }

    private func open(_ manga: SourceManga) {
        launch {
            let id = try await model.viewModel.open(manga: manga)
            navigator.open(.manga(id.int64Value))
        }
    }
}

#Preview("GlobalSearchScreen · Light") { GlobalSearchScreen(query: "").previewScreen().preferredColorScheme(.light) }
#Preview("GlobalSearchScreen · Dark") { GlobalSearchScreen(query: "").previewScreen().preferredColorScheme(.dark) }
