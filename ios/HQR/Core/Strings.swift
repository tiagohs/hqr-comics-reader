import Foundation

// The texts come from Localizable.xcstrings, generated from the shared screens' strings.xml
// (scripts/ios/generate_strings.py): the same key and the same words on every system.

/// The text of [key] in the app language.
func L(_ key: String) -> String {
    NSLocalizedString(key, comment: "")
}

/// [key] with its arguments in order (`%1$@` for text, `%1$lld` for numbers: pass `Int`).
func L(_ key: String, _ args: CVarArg...) -> String {
    String(format: NSLocalizedString(key, comment: ""), arguments: args)
}
