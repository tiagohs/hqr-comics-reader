import Shared
import SwiftUI

/// The state of one core ViewModel for SwiftUI (standard Kotlin/Native export, no SKIE): Kotlin calls back on the main
/// thread, the ViewModel lives while the screen exists and is closed with it.
@MainActor
final class ScreenModel<ViewModel: AnyObject, State: AnyObject>: ObservableObject {
    let viewModel: ViewModel
    @Published private(set) var state: State

    private let screen: Screen<State>
    private var subscription: Subscription?

    init(_ viewModel: ViewModel, flow: (ViewModel) -> Screen<State>) {
        self.viewModel = viewModel
        let screen = flow(viewModel)
        self.screen = screen
        state = screen.current
        subscription = screen.observe { [weak self] newValue in
            MainActor.assumeIsolated { self?.state = newValue }
        }
    }

    deinit {
        subscription?.cancel()
        screen.close()
    }
}

/// Another flow of a ViewModel already held by a ScreenModel (a selection, a choice list): observed while it exists.
@MainActor
final class FlowModel<Value: AnyObject>: ObservableObject {
    @Published private(set) var value: Value
    private var subscription: Subscription?

    init(_ flow: any Kotlinx_coroutines_coreStateFlow) {
        let observer = Observer<Value>(flow: flow)
        value = observer.current
        subscription = observer.observe { [weak self] newValue in
            MainActor.assumeIsolated { self?.value = newValue }
        }
    }

    deinit { subscription?.cancel() }
}

/// The app-wide graph of the core, started once with what only the app knows.
enum Core {
    static var graph: SharedGraph { SharedGraph.shared }

    /// True in the UI tests: an in-memory library and no remote source list.
    static var isUITest: Bool { ProcessInfo.processInfo.arguments.contains("-uiTests") }

    static func start() {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0.0"
        _ = SharedGraph.shared.start(version: version, uiTest: isUITest)
    }
}
