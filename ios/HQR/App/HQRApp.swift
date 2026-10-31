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
            Task { @MainActor in
                Core.graph.downloadScheduler().schedule(restart: false)
                while Core.graph.downloadScheduler().isRunning {
                    try? await Task.sleep(nanoseconds: 1_000_000_000)
                }
                task.setTaskCompleted(success: true)
            }
            task.expirationHandler = { task.setTaskCompleted(success: false) }
        }
    }

    @MainActor
    static func keepGoing() {
        let scheduler = Core.graph.downloadScheduler()
        guard scheduler.isRunning else { return }
        var id = UIBackgroundTaskIdentifier.invalid
        id = UIApplication.shared.beginBackgroundTask(withName: identifier) {
            UIApplication.shared.endBackgroundTask(id)
        }
        Task { @MainActor in
            while scheduler.isRunning { try? await Task.sleep(nanoseconds: 1_000_000_000) }
            UIApplication.shared.endBackgroundTask(id)
        }
        schedule()
    }

    static func schedule() {
        let request = BGProcessingTaskRequest(identifier: identifier)
        request.requiresNetworkConnectivity = true
        try? BGTaskScheduler.shared.submit(request)
    }
}
