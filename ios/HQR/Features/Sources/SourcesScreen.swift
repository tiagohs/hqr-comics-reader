import Shared
import SwiftUI

/// SCR-SRC-001: turn sources on and off, the local folder and the user's servers (RF-03, RF-13 to RF-17, RF-99 to
/// RF-105).
struct SourcesScreen: View {
    @EnvironmentObject private var navigator: Navigator
    @StateObject private var model = ScreenModel(Core.graph.sourcesViewModel()) {
        Screen<SourcesViewModel.State>(viewModel: $0, flow: $0.state)
    }

    var body: some View {
        let state = model.state
        List {
            Section(L("sources_online")) {
                ForEach(state.online, id: \.name) { row in
                    let id = IosBridge.shared.sourceOfRow(row: row)
                    Toggle(isOn: Binding(
                        get: { row.status == .on },
                        set: { IosBridge.shared.setSourceEnabled(viewModel: model.viewModel, sourceId: id, enabled: $0) }
                    )) {
                        Text(row.name)
                        if row.status == .offByList {
                            Text(L("sources_off_by_list"))
                        } else if !row.languages.isEmpty {
                            Text(row.languages.joined(separator: " · ").uppercased())
                        }
                    }
                    .disabled(row.status == .offByList)
                    // RF-06: MangaDex has its own settings (SCR-SRC-002).
                    if id == "mangadex" {
                        NavigationLink(value: Route.sourceSettings) {
                            Label(L("source_settings_open", row.name), systemImage: "slider.horizontal.3")
                        }
                    }
                }
            }
            Section {
                Label(L("source_local"), systemImage: "folder")
                Text(L("settings_local_folder_ios")).font(.caption).foregroundStyle(Palette.onSurfaceVariant)
            } header: {
                Text(L("sources_local"))
            }
            Section(L("sources_servers")) {
                if state.servers.isEmpty {
                    Text(L("sources_servers_empty")).foregroundStyle(Palette.onSurfaceVariant)
                }
                ForEach(state.servers, id: \.url) { server in
                    let id = IosBridge.shared.sourceOfServer(server: server)
                    Button { navigator.open(.serverCatalog(serverId: id)) } label: {
                        VStack(alignment: .leading) {
                            Text(server.name).foregroundStyle(Palette.onSurface)
                            Text(server.url).font(.caption).foregroundStyle(Palette.onSurfaceVariant)
                        }
                    }
                    .swipeActions {
                        Button(role: .destructive) { IosBridge.shared.deleteServer(viewModel: model.viewModel, sourceId: id) } label: {
                            Label(L("server_delete", server.name), systemImage: "trash")
                        }
                        Button { navigator.open(.addServer(serverId: id)) } label: { Label(L("server_edit_title"), systemImage: "pencil") }
                    }
                }
                Button { navigator.open(.addServer(serverId: "")) } label: { Label(L("sources_add_server"), systemImage: "plus") }
            }
        }
        .navigationTitle(L("sources_title"))
    }
}

/// SCR-SRV-001: an OPDS server (Komga, Kavita, calibre-web), tested before it is saved; the password goes to the
/// Keychain (RF-17 to RF-19, RF-95, RF-96).
struct AddServerScreen: View {
    let serverId: String
    @EnvironmentObject private var navigator: Navigator
    @StateObject private var model: ScreenModel<AddServerViewModel, AddServerViewModel.State>

    init(serverId: String) {
        self.serverId = serverId
        _model = StateObject(wrappedValue: ScreenModel(Core.graph.addServerViewModel(serverId: serverId)) {
            Screen<AddServerViewModel.State>(viewModel: $0, flow: $0.state)
        })
    }

    // Typed here and sent to the ViewModel: a field bound to the core's state would lose fast keystrokes.
    @State private var name = ""
    @State private var url = ""
    @State private var username = ""
    @State private var password = ""
    @State private var typed = false

    var body: some View {
        let state = model.state
        Form {
            Section {
                TextField(L("server_name"), text: $name)
                TextField(L("server_url"), text: $url)
                    .keyboardType(.URL)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            } footer: { Text(L("server_url_hint")) }
            Section {
                TextField(L("server_username"), text: $username)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                SecureField(L("server_password"), text: $password)
            } footer: { Text(L("server_saved_secure")) }
            Section {
                Button(L("server_test")) { model.viewModel.test() }.disabled(state.busy)
                if IosBridge.shared.serverOk(state: state) {
                    Label(L("server_ok"), systemImage: "checkmark.circle").foregroundStyle(.green)
                } else if let problem = IosBridge.shared.serverProblem(state: state) {
                    Text(problemText(problem)).foregroundStyle(Palette.error)
                }
            }
        }
        .navigationTitle(state.isEdit ? L("server_edit_title") : L("server_add_title"))
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button(L("action_save")) { model.viewModel.save() }.disabled(state.busy)
            }
        }
        .overlay { if state.busy { ProgressView() } }
        .onAppear { load(model.state.input) }
        // Editing a saved server: its data arrives after the screen opens.
        .onChange(of: state.input) { _, input in if !typed { load(input) } }
        .onChange(of: [name, url, username, password]) { _, _ in
            typed = true
            model.viewModel.edit(input: ServerInput(name: name, url: url, username: username, password: password))
        }
        .onChange(of: state.saved) { _, saved in if saved { navigator.back() } }
    }

    private func load(_ input: ServerInput) {
        name = input.name
        url = input.url
        username = input.username
        typed = false
    }

    private func problemText(_ problem: ServerProblem) -> String {
        switch problem {
        case .invalidAddress: L("server_problem_invalid_address")
        case .credentialsInAddress: L("server_problem_credentials_in_address")
        case .cleartextOutsideHome: L("server_problem_cleartext")
        case .unreachable: L("server_problem_unreachable")
        case .wrongLogin: L("server_problem_wrong_login")
        case .certificate: L("server_problem_certificate")
        default: L("server_problem_not_opds")
        }
    }
}

/// SCR-SRV-002: the server's folders and books, with its search; a book opens as a work (RF-18, RF-20).
struct ServerCatalogScreen: View {
    let serverId: String
    @EnvironmentObject private var navigator: Navigator
    @StateObject private var model: ScreenModel<ServerCatalogViewModel, ServerCatalogViewModel.State>
    @State private var query = ""

    init(serverId: String) {
        self.serverId = serverId
        _model = StateObject(wrappedValue: ScreenModel(Core.graph.serverCatalogViewModel(serverId: serverId)) {
            Screen<ServerCatalogViewModel.State>(viewModel: $0, flow: $0.state)
        })
    }

    var body: some View {
        let state = model.state
        List {
            if let error = state.error {
                ErrorBox(message: error.message, action: L("action_retry")) { model.viewModel.retry() }
                    .listRowBackground(Color.clear)
            }
            if let page = state.page {
                ForEach(page.entries, id: \.url) { entry in
                    Button {
                        model.viewModel.open(entry: entry) { id in navigator.open(.manga(id.int64Value)) }
                    } label: {
                        Label {
                            VStack(alignment: .leading) {
                                Text(entry.title).foregroundStyle(Palette.onSurface)
                                if let subtitle = entry.subtitle { Text(subtitle).font(.caption).foregroundStyle(Palette.onSurfaceVariant) }
                            }
                        } icon: {
                            Image(systemName: entry.isBook ? "book.closed" : "folder")
                        }
                    }
                }
                if page.nextUrl != nil {
                    Button(L("catalog_more")) { model.viewModel.loadMore() }.disabled(state.loadingMore)
                }
            }
        }
        .overlay { if state.loading { ProgressView() } }
        .navigationTitle(state.page?.title ?? state.serverName)
        .navigationBarBackButtonHidden(state.canGoUp)
        .toolbar {
            if state.canGoUp {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button { _ = model.viewModel.up() } label: { Image(systemName: "chevron.backward") }
                        .accessibilityLabel(L("action_back"))
                }
            }
        }
        .modifier(SearchIf(enabled: state.page?.canSearch ?? false, query: $query, prompt: L("search_hint")) {
            model.viewModel.search(query: query)
        })
    }
}

#Preview("SourcesScreen · Light") { SourcesScreen().previewScreen().preferredColorScheme(.light) }
#Preview("SourcesScreen · Dark") { SourcesScreen().previewScreen().preferredColorScheme(.dark) }
#Preview("AddServerScreen · Light") { AddServerScreen(serverId: "").previewScreen().preferredColorScheme(.light) }
#Preview("AddServerScreen · Dark") { AddServerScreen(serverId: "").previewScreen().preferredColorScheme(.dark) }
#Preview("ServerCatalogScreen · Light") { ServerCatalogScreen(serverId: "opds-1").previewScreen().preferredColorScheme(.light) }
#Preview("ServerCatalogScreen · Dark") { ServerCatalogScreen(serverId: "opds-1").previewScreen().preferredColorScheme(.dark) }
