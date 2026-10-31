import Shared
import SwiftUI

/// SCR-LIB-003: create, rename, order and delete categories (RF-53).
struct CategoriesScreen: View {
    @StateObject private var model = ScreenModel(Core.graph.categoriesViewModel()) {
        Screen<CategoriesViewModel.State>(viewModel: $0, flow: $0.state)
    }
    @State private var newName = ""
    @State private var renaming: Shared.Category?
    @State private var renameText = ""

    var body: some View {
        let state = model.state
        List {
            Section {
                HStack {
                    TextField(L("library_categories"), text: $newName)
                        .submitLabel(.done)
                        .onSubmit(create)
                    Button(L("action_save"), action: create)
                        .disabled(model.viewModel.problem(name: newName, except: nil) != nil)
                }
            }
            Section {
                ForEach(Array(state.categories.enumerated()), id: \.element.id) { index, category in
                    HStack {
                        VStack(alignment: .leading) {
                            Text(category.name)
                            let count = state.counts[KotlinLong(value: category.id)]?.intValue ?? 0
                            Text(P("categories_works", count)).font(.caption).foregroundStyle(Palette.onSurfaceVariant)
                        }
                        Spacer()
                        Button { model.viewModel.move(id: category.id, by: -1) } label: { Image(systemName: "chevron.up") }
                            .disabled(index == 0)
                            .accessibilityLabel(L("action_move_up"))
                        Button { model.viewModel.move(id: category.id, by: 1) } label: { Image(systemName: "chevron.down") }
                            .disabled(index == state.categories.count - 1)
                            .accessibilityLabel(L("action_move_down"))
                    }
                    .buttonStyle(.borderless)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        renaming = category
                        renameText = category.name
                    }
                    .swipeActions {
                        Button(role: .destructive) {
                            model.viewModel.delete(id: category.id) { _ in }
                        } label: { Label(L("history_remove"), systemImage: "trash") }
                    }
                }
            }
        }
        .navigationTitle(L("library_categories"))
        .alert(L("manga_edit_categories"), isPresented: Binding(get: { renaming != nil }, set: { if !$0 { renaming = nil } })) {
            TextField(L("library_categories"), text: $renameText)
            Button(L("action_save")) {
                if let category = renaming { model.viewModel.rename(id: category.id, name: renameText) }
                renaming = nil
            }
            Button(L("action_cancel"), role: .cancel) { renaming = nil }
        }
    }

    private func create() {
        guard model.viewModel.problem(name: newName, except: nil) == nil else { return }
        model.viewModel.create(name: newName)
        newName = ""
    }
}

#Preview("CategoriesScreen · Light") { CategoriesScreen().previewScreen().preferredColorScheme(.light) }
#Preview("CategoriesScreen · Dark") { CategoriesScreen().previewScreen().preferredColorScheme(.dark) }
