import Shared
import SwiftUI

/// SCR-LIB-002: filter, sort and display, saved as they change (RF-54, RF-55). The rules live in the shared
/// LibraryOptions, the same as on Android.
struct LibraryOptionsSheet: View {
    let state: LibraryViewModel.State
    let onChange: (LibraryOptions) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var tab = 0

    private var options: LibraryOptions { state.options }

    var body: some View {
        NavigationStack {
            Form {
                Picker("", selection: $tab) {
                    Text(L("library_tab_filter")).tag(0)
                    Text(L("library_tab_sort")).tag(1)
                    Text(L("library_tab_display")).tag(2)
                }
                .pickerStyle(.segmented)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())
                switch tab {
                case 0: filters
                case 1: sort
                default: display
                }
            }
            .navigationTitle(L("library_options"))
            .navigationBarTitleDisplayMode(.inline)
            .safeAreaInset(edge: .bottom) {
                Button { dismiss() } label: {
                    Text(L("library_show_works", Int(state.items.count))).font(.headline).frame(maxWidth: .infinity).frame(height: 44)
                }
                .buttonStyle(.borderedProminent)
                .padding()
                .background(.bar)
            }
        }
        .presentationDetents([.medium, .large])
    }

    // MARK: Filter

    @ViewBuilder private var filters: some View {
        Section(L("library_label_state")) {
            stateToggle(.unread, "library_filter_unread")
            stateToggle(.downloaded, "library_filter_downloaded")
            stateToggle(.started, "library_filter_started")
            stateToggle(.ongoing, "library_filter_ongoing")
            stateToggle(.completed, "library_filter_completed")
        }
        if state.sourceIds.count > 1 {
            Section(L("library_label_source")) {
                ForEach(state.sourceIds, id: \.self) { id in
                    Toggle(sourceDisplayName(id), isOn: Binding(
                        get: { options.sources.contains(id) },
                        set: { _ in onChange(options.toggleSource(id: id)) }
                    ))
                }
            }
        }
        if !state.categories.isEmpty {
            Section(L("library_label_category")) {
                ForEach(state.categories, id: \.id) { category in
                    categoryToggle(category.name, id: category.id)
                }
                categoryToggle(L("library_tab_uncategorized"), id: Shared.Category.Companion.shared.DEFAULT_ID)
            }
        }
        if options.hasFilters {
            Button(L("library_clear_filters")) { onChange(options.withoutFilters()) }
        }
    }

    private func stateToggle(_ filter: LibraryOptions.StateFilter, _ key: String) -> some View {
        Toggle(L(key), isOn: Binding(get: { options.isOn(filter: filter) }, set: { _ in onChange(options.toggle(filter: filter)) }))
    }

    private func categoryToggle(_ name: String, id: Int64) -> some View {
        Toggle(name, isOn: Binding(
            get: { options.categories.contains(KotlinLong(value: id)) },
            set: { _ in onChange(options.toggleCategory(id: id)) }
        ))
    }

    // MARK: Sort

    @ViewBuilder private var sort: some View {
        Section {
            Picker(L("library_tab_sort"), selection: Binding(get: { options.sort }, set: { onChange(options.withSort(value: $0)) })) {
                Text(L("library_sort_title")).tag(LibrarySort.title)
                Text(L("library_sort_last_read")).tag(LibrarySort.lastRead)
                Text(L("library_sort_latest_chapter")).tag(LibrarySort.latestChapter)
                Text(L("library_sort_total")).tag(LibrarySort.totalChapters)
                Text(L("library_sort_unread")).tag(LibrarySort.unread)
                Text(L("library_sort_added")).tag(LibrarySort.dateAdded)
            }
            .pickerStyle(.inline)
            .labelsHidden()
        }
        Section {
            Picker("", selection: Binding(get: { options.ascending }, set: { onChange(options.withAscending(value: $0)) })) {
                Text(L("library_sort_ascending")).tag(true)
                Text(L("library_sort_descending")).tag(false)
            }
            .pickerStyle(.segmented)
        }
    }

    // MARK: Display

    @ViewBuilder private var display: some View {
        Section {
            Picker("", selection: Binding(get: { options.display }, set: { onChange(options.withDisplay(value: $0)) })) {
                Text(L("library_display_compact")).tag(LibraryDisplay.compactGrid)
                Text(L("library_display_comfortable")).tag(LibraryDisplay.comfortableGrid)
                Text(L("library_display_list")).tag(LibraryDisplay.list)
            }
            .pickerStyle(.segmented)
        }
        Section {
            badgeToggle(.unread, "library_badge_unread")
            badgeToggle(.downloaded, "library_badge_downloaded")
            badgeToggle(.source, "library_badge_source")
        }
    }

    private func badgeToggle(_ badge: LibraryOptions.Badge, _ key: String) -> some View {
        Toggle(L(key), isOn: Binding(get: { options.badgeOn(badge: badge) }, set: { onChange(options.withBadge(badge: badge, on: $0)) }))
    }
}

/// RF-53: the categories a work is in; one or more.
struct CategoryPicker: View {
    let categories: [Shared.Category]
    let assigned: Set<Int64>
    let onSave: (Set<Int64>) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var chosen: Set<Int64> = []

    var body: some View {
        NavigationStack {
            List(categories, id: \.id) { category in
                Button {
                    if chosen.contains(category.id) { chosen.remove(category.id) } else { chosen.insert(category.id) }
                } label: {
                    HStack {
                        Text(category.name).foregroundStyle(Palette.onSurface)
                        Spacer()
                        if chosen.contains(category.id) { Image(systemName: "checkmark").foregroundStyle(.tint) }
                    }
                }
                .accessibilityAddTraits(chosen.contains(category.id) ? .isSelected : [])
            }
            .navigationTitle(L("manga_categories_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(L("action_cancel")) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L("action_save")) {
                        onSave(chosen)
                        dismiss()
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .onAppear { chosen = assigned }
    }
}

#Preview("LibraryOptionsSheet · Light") {
    LibraryOptionsSheet(state: Core.graph.libraryViewModel().state.value as! LibraryViewModel.State, onChange: { _ in }).previewEnvironment().preferredColorScheme(.light)
}
#Preview("LibraryOptionsSheet · Dark") {
    LibraryOptionsSheet(state: Core.graph.libraryViewModel().state.value as! LibraryViewModel.State, onChange: { _ in }).previewEnvironment().preferredColorScheme(.dark)
}
#Preview("CategoryPicker · Light") {
    CategoryPicker(categories: [], assigned: [], onSave: { _ in }).preferredColorScheme(.light)
}
#Preview("CategoryPicker · Dark") {
    CategoryPicker(categories: [], assigned: [], onSave: { _ in }).preferredColorScheme(.dark)
}
