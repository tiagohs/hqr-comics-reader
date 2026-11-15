import Shared
import SwiftUI

/// SCR-OBR-002: find the same work in another source and move categories, read chapters and history to it (RF-69 to
/// RF-71).
struct MigrationScreen: View {
    let mangaId: Int64
    @EnvironmentObject private var navigator: Navigator
    @StateObject private var model: ScreenModel<MigrationViewModel, MigrationViewModel.State>
    @State private var deleteDownloads = false
    @State private var query = ""

    init(mangaId: Int64) {
        self.mangaId = mangaId
        _model = StateObject(wrappedValue: ScreenModel(Core.graph.migrationViewModel(mangaId: mangaId)) {
            Screen<MigrationViewModel.State>(viewModel: $0, flow: $0.state)
        })
    }

    var body: some View {
        let state = model.state
        List {
            // RF-70: what goes with the work, before the person picks a candidate.
            (Text(L("migrate_carries") + " ").bold() + Text(L("migrate_carries_detail")))
                .font(.footnote)
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Palette.surface)
                .overlay(Rectangle().stroke(Palette.onSurface, lineWidth: 2))
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
            Section(L("migrate_query")) {
                TextField(L("migrate_query"), text: $query)
                    .submitLabel(.search)
                    .onSubmit { model.viewModel.search() }
                    .onChange(of: query) { _, text in model.viewModel.setQuery(query: text) }
            }
            Section(L("migrate_sources")) {
                if state.sources.isEmpty {
                    Text(L("migrate_no_sources")).foregroundStyle(Palette.onSurfaceVariant)
                }
                ForEach(state.sources, id: \.name) { option in
                    Toggle(sourceDisplayName(IosBridge.shared.sourceOfOption(option: option)), isOn: Binding(
                        get: { option.chosen },
                        set: { _ in IosBridge.shared.toggleMigrationSource(viewModel: model.viewModel, sourceId: IosBridge.shared.sourceOfOption(option: option)) }
                    ))
                }
            }
            ForEach(state.sources.filter(\.chosen), id: \.name) { option in
                let id = IosBridge.shared.sourceOfOption(option: option)
                if let results = IosBridge.shared.migrationResults(state: state, sourceId: id) {
                    Section(sourceDisplayName(id)) { resultRows(results) }
                }
            }
            if let error = state.error {
                ErrorBox(message: error.message).listRowBackground(Color.clear)
            }
        }
        .navigationTitle(L("migrate_title", state.title))
        .navigationBarTitleDisplayMode(.inline)
        .overlay { if state.migrating { ProgressView() } }
        .alert(L("migrate_confirm_title", state.picked?.title ?? ""),
               isPresented: Binding(get: { model.state.picked != nil }, set: { if !$0 { model.viewModel.pick(candidate: nil) } })) {
            Button(L("migrate_confirm")) { model.viewModel.migrate(deleteDownloads: deleteDownloads) }
            Button(L("migrate_delete_downloads")) {
                deleteDownloads = true
                model.viewModel.migrate(deleteDownloads: true)
            }
            Button(L("action_cancel"), role: .cancel) { model.viewModel.pick(candidate: nil) }
        } message: {
            Text(L("migrate_confirm_message", state.picked?.title ?? ""))
        }
        .onChange(of: state.done) { _, done in
            // The new work replaces the old one in the stack.
            if let done {
                navigator.back()
                navigator.back()
                navigator.open(.manga(done.mangaId))
            }
        }
        .onAppear {
            query = model.state.query
            if state.results.isEmpty { model.viewModel.search() }
        }
        .onChange(of: state.title) { _, _ in if query.isEmpty { query = model.state.query } }
    }

    @ViewBuilder
    private func resultRows(_ results: MigrationViewModel.Results) -> some View {
        if results.loading {
            ProgressView()
        } else if let error = results.error {
            Text(error.message).foregroundStyle(Palette.error)
        } else if results.items.isEmpty {
            Text(L("migrate_nothing")).foregroundStyle(Palette.onSurfaceVariant)
        } else {
            ForEach(results.items, id: \.listId) { manga in
                Button { model.viewModel.pick(candidate: manga) } label: {
                    HStack(spacing: 12) {
                        CoverImage(manga: manga, width: 44)
                        VStack(alignment: .leading) {
                            Text(manga.title).foregroundStyle(Palette.onSurface)
                            if !manga.authors.isEmpty {
                                Text(manga.authors.joined(separator: ", ")).font(.caption).foregroundStyle(Palette.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

#Preview("MigrationScreen · Light") { MigrationScreen(mangaId: 1).previewScreen().preferredColorScheme(.light) }
#Preview("MigrationScreen · Dark") { MigrationScreen(mangaId: 1).previewScreen().preferredColorScheme(.dark) }
