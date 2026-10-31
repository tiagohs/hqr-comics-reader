import BackgroundTasks
import Shared
import SwiftUI

@main
struct HQRApp: App {
    @Environment(\.scenePhase) private var phase

    init() {
        Core.start()
        BackgroundDownloads.register()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
        .onChange(of: phase) { _, now in
            // RNF-28: the queue keeps going for the time the system gives, then asks for a processing task.
            if now == .background { BackgroundDownloads.keepGoing() }
        }
    }
}

/// RNF-28: downloads continue while the app is open, for the extra time iOS gives after it leaves the screen, and in a
/// BGProcessingTask with network when the system allows; the difference to Android is said on the downloads screen.
enum BackgroundDownloads {
    static let identifier = "com.tiagohs.hqr.reader.downloads"

    static func register() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: identifier, using: nil) { task in
            guard let task = task as? BGProcessingTask else { return }
            schedule()
            let finish = Once { task.setTaskCompleted(success: $0) }
            task.expirationHandler = { finish.run(false) }
            Task { @MainActor in
                Core.graph.downloadScheduler().schedule(restart: false)
                while Core.graph.downloadScheduler().isRunning && !finish.done {
                    try? await Task.sleep(nanoseconds: 1_000_000_000)
                }
                finish.run(true)
            }
        }
    }

    @MainActor
    static func keepGoing() {
        let scheduler = Core.graph.downloadScheduler()
        guard scheduler.isRunning else { return }
        let task = BackgroundTask()
        task.begin(name: identifier)
        Task { @MainActor in
            while scheduler.isRunning && task.active { try? await Task.sleep(nanoseconds: 1_000_000_000) }
            task.end()
        }
        schedule()
    }

    static func schedule() {
        let request = BGProcessingTaskRequest(identifier: identifier)
        request.requiresNetworkConnectivity = true
        try? BGTaskScheduler.shared.submit(request)
    }
}

/// Runs its action once, whoever calls first (the system's expiration or the end of the queue).
final class Once: @unchecked Sendable {
    private let lock = NSLock()
    private let action: (Bool) -> Void
    private(set) var done = false

    init(_ action: @escaping (Bool) -> Void) { self.action = action }

    func run(_ value: Bool) {
        lock.lock()
        let first = !done
        done = true
        lock.unlock()
        if first { action(value) }
    }
}

/// The extra time iOS gives after the app leaves the screen, ended exactly once.
@MainActor
final class BackgroundTask {
    private var id = UIBackgroundTaskIdentifier.invalid
    var active: Bool { id != .invalid }

    func begin(name: String) {
        id = UIApplication.shared.beginBackgroundTask(withName: name) { [weak self] in
            MainActor.assumeIsolated { self?.end() }
        }
    }

    func end() {
        guard id != .invalid else { return }
        UIApplication.shared.endBackgroundTask(id)
        id = .invalid
    }
}
