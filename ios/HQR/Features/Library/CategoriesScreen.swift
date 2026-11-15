import Shared
import SwiftUI

/// SCR-LIB-003: create, rename, order and delete categories (RF-53); deleting can be undone.
struct CategoriesScreen: View {
    @StateObject private var model = ScreenModel(Core.graph.categoriesViewModel()) {
        Screen<CategoriesViewModel.State>(viewModel: $0, flow: $0.state)
    }
    @State private var newName = ""
    @State private var renaming: Shared.Category?
    @State private var renameText = ""
    @State private var deleted: DeletedCategory?

    var body: some View {
        let state = model.state
        List {
            Section {
                HStack {
                    TextField(L("categories_new"), text: $newName)
                        .submitLabel(.done)
                        .onSubmit(create)
                    Button(L("action_save"), action: create)
                        .disabled(model.viewModel.problem(name: newName, except: nil) != nil)
                }
            } footer: {
                if !newName.isEmpty, let problem = model.viewModel.problem(name: newName, except: nil) {
                    Text(message(problem)).foregroundStyle(Palette.error)
                }
            }
            Section {
                if state.categories.isEmpty {
                    Text(L("categories_empty")).foregroundStyle(Palette.onSurfaceVariant)
                }
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
                            .accessibilityLabel(L("action_move_up", category.name))
                        Button { model.viewModel.move(id: category.id, by: 1) } label: { Image(systemName: "chevron.down") }
                            .disabled(index == state.categories.count - 1)
                            .accessibilityLabel(L("action_move_down", category.name))
                    }
                    .buttonStyle(.borderless)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        renaming = category
                        renameText = category.name
                    }
                    .accessibilityAction(named: L("categories_rename")) {
                        renaming = category
                        renameText = category.name
                    }
                    .swipeActions {
                        Button(role: .destructive) {
                            model.viewModel.delete(id: category.id) { removal in deleted = removal }
                        } label: { Label(L("categories_delete"), systemImage: "trash") }
                    }
                }
            } footer: {
                if !state.categories.isEmpty {
                    Text(L("categories_hint") + "\n" + L("categories_uncategorized_hint"))
                }
            }
        }
        .navigationTitle(L("library_categories"))
        .alert(L("categories_rename"), isPresented: Binding(get: { renaming != nil }, set: { if !$0 { renaming = nil } })) {
            TextField(L("categories_name"), text: $renameText)
            Button(L("action_save")) {
                if let category = renaming, model.viewModel.problem(name: renameText, except: KotlinLong(value: category.id)) == nil {
                    model.viewModel.rename(id: category.id, name: renameText)
                }
                renaming = nil
            }
            Button(L("action_cancel"), role: .cancel) { renaming = nil }
        } message: {
            if let category = renaming, let problem = model.viewModel.problem(name: renameText, except: KotlinLong(value: category.id)) {
                Text(message(problem))
            }
        }
        .safeAreaInset(edge: .bottom) {
            if let removal = deleted {
                UndoBar(text: L("categories_deleted", removal.category.name, removal.mangaIds.count)) {
                    model.viewModel.undoDelete(deleted: removal)
                    deleted = nil
                } dismiss: { deleted = nil }
                .id(removal.category.id)
            }
        }
    }

    private func message(_ problem: CategoriesViewModel.NameProblem) -> String {
        problem == .taken ? L("categories_name_taken") : L("categories_name_empty")
    }

    private func create() {
        guard model.viewModel.problem(name: newName, except: nil) == nil else { return }
        model.viewModel.create(name: newName)
        newName = ""
    }
}

#Preview("CategoriesScreen · Light") { CategoriesScreen().previewScreen().preferredColorScheme(.light) }
#Preview("CategoriesScreen · Dark") { CategoriesScreen().previewScreen().preferredColorScheme(.dark) }
