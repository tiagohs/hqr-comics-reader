import Shared
import SwiftUI
import UniformTypeIdentifiers

/// SCR-SET-002: create a `.hqrbackup` and restore one, the same file as on Android (RF-72 to RF-75, RF-89).
struct BackupScreen: View {
    @StateObject private var model = ScreenModel(Core.graph.backupViewModel()) {
        Screen<BackupViewModel.State>(viewModel: $0, flow: $0.state)
    }
    @State private var importing = false

    var body: some View {
        let state = model.state
        List {
            Section {
                Button { model.viewModel.create() } label: {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(L("backup_create"))
                        Text(L("backup_create_hint")).font(.caption).foregroundStyle(Palette.onSurfaceVariant)
                    }
                }
                Button { importing = true } label: {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(L("backup_restore"))
                        Text(L("backup_restore_hint")).font(.caption).foregroundStyle(Palette.onSurfaceVariant)
                    }
                }
            }
            .disabled(state.busy)
            if let saved = state.saved {
                Text(saved.boolValue ? L("backup_saved") : L("backup_not_saved"))
                    .foregroundStyle(saved.boolValue ? Palette.onSurface : Palette.error)
            }
            if let summary = state.summary {
                Section(L("backup_restored_title")) {
                    Text(L("backup_restored", Int(summary.works), Int(summary.chaptersRead), Int(summary.categories), Int(summary.servers)))
                    if summary.servers > 0 { Text(L("backup_restored_servers")).font(.caption) }
                    if !summary.unavailableSources.isEmpty {
                        Text(L("backup_unavailable", summary.unavailableSources.joined(separator: ", "))).font(.caption)
                    }
                }
            }
            if let problem = state.problem {
                ErrorBox(message: problem == .newerVersion ? L("backup_newer") : L("backup_not_a_backup"))
                    .listRowBackground(Color.clear)
            }
        }
        .navigationTitle(L("backup_title"))
        .overlay { if state.busy { ProgressView() } }
        .fileExporter(
            isPresented: Binding(get: { model.state.pending != nil }, set: { if !$0 && model.state.pending != nil { model.viewModel.saved(ok: false) } }),
            document: BackupFile(data: state.pending.map { Core.graph.data(bytes: $0) } ?? Data()),
            contentType: .hqrBackup,
            defaultFilename: model.viewModel.fileName
        ) { result in
            model.viewModel.saved(ok: (try? result.get()) != nil)
        }
        .fileImporter(isPresented: $importing, allowedContentTypes: [.hqrBackup, .gzip, .data]) { result in
            guard let url = try? result.get() else { return }
            let scoped = url.startAccessingSecurityScopedResource()
            defer { if scoped { url.stopAccessingSecurityScopedResource() } }
            let bytes = (try? Data(contentsOf: url)).map { Core.graph.bytes(data: $0) }
            model.viewModel.restore(bytes: bytes)
        }
    }
}

extension UTType {
    /// `.hqrbackup`: gzip with JSON inside (ADR-010).
    static let hqrBackup = UTType(exportedAs: "com.tiagohs.hqr.backup", conformingTo: .gzip)
}

/// The backup bytes as a document the system's save panel writes.
struct BackupFile: FileDocument {
    static var readableContentTypes: [UTType] { [.hqrBackup] }
    let data: Data

    init(data: Data) { self.data = data }

    init(configuration: ReadConfiguration) throws {
        data = configuration.file.regularFileContents ?? Data()
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}

#Preview("BackupScreen · Light") { BackupScreen().previewScreen().preferredColorScheme(.light) }
#Preview("BackupScreen · Dark") { BackupScreen().previewScreen().preferredColorScheme(.dark) }
