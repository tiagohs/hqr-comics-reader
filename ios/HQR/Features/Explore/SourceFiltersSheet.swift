import Shared
import SwiftUI

/// SCR-EXP-003: the source's filters, with "Ver resultados" fixed at the bottom (RF-22). Tags cycle off → included →
/// excluded; an excluded tag reads "Sem Romance" with the "−" mark. The rules are the shared SourceFilter's.
struct SourceFiltersSheet: View {
    let defaults: [SourceFilter]
    let onApply: ([SourceFilter]) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var draft: [SourceFilter]

    init(filters: [SourceFilter], defaults: [SourceFilter], onApply: @escaping ([SourceFilter]) -> Void) {
        self.defaults = defaults
        self.onApply = onApply
        _draft = State(initialValue: filters)
    }

    var body: some View {
        NavigationStack {
            Form {
                ForEach(draft, id: \.key) { filter in
                    Section {
                        group(filter)
                    } header: {
                        Text(Self.groupTitle(filter.key))
                    } footer: {
                        if filter is SourceFilterTags { Text(L("filter_tags_hint")) }
                    }
                }
            }
            .navigationTitle(L("filter_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(L("action_cancel")) { dismiss() } }
                ToolbarItem(placement: .primaryAction) {
                    Button(L("filter_reset")) { if !defaults.isEmpty { draft = defaults } }
                }
            }
            .safeAreaInset(edge: .bottom) {
                Button {
                    onApply(draft)
                    dismiss()
                } label: {
                    Text(L("filter_apply")).font(.headline).frame(maxWidth: .infinity).frame(height: 44)
                }
                .buttonStyle(.borderedProminent)
                .padding()
                .background(.bar)
            }
        }
    }

    private func change(_ filter: SourceFilter) {
        draft = SourceFilterCompanion.shared.replace(filters: draft, changed: filter)
    }

    @ViewBuilder
    private func group(_ filter: SourceFilter) -> some View {
        if let tags = filter as? SourceFilterTags {
            FlowLayout(spacing: 8) {
                ForEach(tags.options, id: \.value) { option in tagChip(tags, option) }
            }
            .padding(.vertical, 4)
        } else if let choice = filter as? SourceFilterChoice {
            FlowLayout(spacing: 8) {
                ForEach(choice.options, id: \.value) { option in
                    let on = choice.selected.contains(option.value)
                    chip(Self.optionLabel(option), systemImage: on ? "checkmark" : nil, on: on) {
                        change(choice.toggle(value: option.value))
                    }
                }
            }
            .padding(.vertical, 4)
        } else if let text = filter as? SourceFilterText {
            TextField(Self.groupTitle(text.key), text: Binding(get: { text.value }, set: { change(text.typed(text: $0)) }))
                .keyboardType(.numberPad)
        } else if let sort = filter as? SourceFilterSort {
            Picker(Self.groupTitle(sort.key), selection: Binding(get: { sort.selected ?? "" }, set: { change(sort.select(value: $0)) })) {
                ForEach(sort.options, id: \.value) { option in Text(Self.optionLabel(option)).tag(option.value) }
            }
            .pickerStyle(.inline)
            .labelsHidden()
            Picker("", selection: Binding(get: { sort.ascending }, set: { change(sort.direction(ascending: $0)) })) {
                Text(L("library_sort_ascending")).tag(true)
                Text(L("library_sort_descending")).tag(false)
            }
            .pickerStyle(.segmented)
        }
    }

    private func tagChip(_ tags: SourceFilterTags, _ option: SourceFilterOption) -> some View {
        let included = tags.included.contains(option.value)
        let excluded = tags.excluded.contains(option.value)
        return chip(excluded ? L("filter_without", option.label) : option.label,
                    systemImage: included ? "checkmark" : excluded ? "minus" : nil,
                    on: included || excluded,
                    excluded: excluded) {
            change(tags.cycle(value: option.value))
        }
        // design.md "Filtros": an excluded tag reads as such.
        .accessibilityValue(excluded ? L("filter_excluded") : "")
    }

    private func chip(_ text: String, systemImage: String?, on: Bool, excluded: Bool = false, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 4) {
                if let systemImage { Image(systemName: systemImage).font(.caption.weight(.bold)) }
                Text(text).font(.subheadline)
            }
            .padding(.horizontal, 12).padding(.vertical, 6)
            .background(on ? (excluded ? Palette.surfaceHighest : Palette.primaryContainer) : Color.clear, in: Capsule())
            .overlay(Capsule().stroke(on ? Color.clear : Palette.outline, lineWidth: 1))
            .foregroundStyle(on && !excluded ? Palette.onPrimaryContainer : Palette.onSurface)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(on ? .isSelected : [])
    }

    /// Known keys are named in the app language; a source's own words (tags, demographics) stay as given.
    static func groupTitle(_ key: String) -> String {
        let titles = ["tags": "filter_tags", "status": "filter_status", "demographic": "filter_demographic",
                      "originalLanguage": "filter_original_language", "year": "filter_year", "order": "filter_order"]
        return titles[key].map { L($0) } ?? key
    }

    static func optionLabel(_ option: SourceFilterOption) -> String {
        let labels = ["ongoing": "status_ongoing", "completed": "status_completed", "hiatus": "status_hiatus",
                      "cancelled": "status_cancelled", "none": "demographic_none", "ja": "language_ja", "ko": "language_ko",
                      "zh": "language_zh", "en": "language_en", "pt-br": "language_pt_br", "relevance": "order_relevance",
                      "latestUploadedChapter": "order_latest_chapter", "followedCount": "order_followed",
                      "createdAt": "order_created", "title": "order_title", "year": "order_year", "rating": "order_rating"]
        return labels[option.value].map { L($0) } ?? option.label.prefix(1).uppercased() + option.label.dropFirst()
    }
}

#Preview("SourceFiltersSheet · Light") {
    SourceFiltersSheet(filters: [], defaults: [], onApply: { _ in }).preferredColorScheme(.light)
}
#Preview("SourceFiltersSheet · Dark") {
    SourceFiltersSheet(filters: [], defaults: [], onApply: { _ in }).preferredColorScheme(.dark)
}
