#!/usr/bin/env python3
"""The Xcode project of the iOS app: ios/HQR.xcodeproj (project.pbxproj and the shared scheme).

The project uses folders synchronized with the disk (Xcode 16+): a new Swift file, image or text inside ios/HQR
enters the app without touching the project, so this script only runs when targets, phases or settings change. `--check` fails when the
committed project is not what this script writes.
"""

import hashlib
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PROJECT = ROOT / "ios/HQR.xcodeproj"
APP, TESTS, UI_TESTS = "HQR", "HQRTests", "HQRUITests"
BUNDLE_ID = "com.tiagohs.hqr.reader"
KOTLIN = 'cd "$SRCROOT/.."\\nif [ "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" = "YES" ]; then exit 0; fi\\n./gradlew :shared:app:embedAndSignAppleFrameworkForXcode -Phqr.ios=true\\n'


def uid(*parts):
    return hashlib.md5("/".join(parts).encode()).hexdigest()[:24].upper()


def plist(value, indent=2):
    tab = "\t" * indent
    if isinstance(value, dict):
        body = "".join(f"{tab}\t{k} = {plist(v, indent + 1)};\n" for k, v in value.items())
        return "{\n" + body + tab + "}"
    if isinstance(value, list):
        body = "".join(f"{tab}\t{plist(v, indent + 1)},\n" for v in value)
        return "(\n" + body + tab + ")"
    text = str(value)
    if text and all(c.isalnum() or c in "._/$" for c in text) and not text.startswith("$("):
        return text
    return '"' + text.replace("\\", "\\\\").replace('"', '\\"').replace("\\\\n", "\\n") + '"'


COMMON = {
    "ALWAYS_SEARCH_USER_PATHS": "NO",
    "ASSETCATALOG_COMPILER_GENERATE_SWIFT_ASSET_SYMBOL_EXTENSIONS": "YES",
    "CLANG_ENABLE_MODULES": "YES",
    "CLANG_ENABLE_OBJC_ARC": "YES",
    "ENABLE_STRICT_OBJC_MSGSEND": "YES",
    "ENABLE_USER_SCRIPT_SANDBOXING": "NO",
    "GCC_C_LANGUAGE_STANDARD": "gnu17",
    "IPHONEOS_DEPLOYMENT_TARGET": "17.0",
    "LOCALIZATION_PREFERS_STRING_CATALOGS": "YES",
    "SDKROOT": "iphoneos",
    "STRING_CATALOG_GENERATE_SYMBOLS": "NO",
    "SWIFT_EMIT_LOC_STRINGS": "YES",
    "SWIFT_VERSION": "5.0",
    "TARGETED_DEVICE_FAMILY": "1,2",
}
DEBUG = {**COMMON, "DEBUG_INFORMATION_FORMAT": "dwarf", "ENABLE_TESTABILITY": "YES", "GCC_OPTIMIZATION_LEVEL": "0",
         "ONLY_ACTIVE_ARCH": "YES", "SWIFT_ACTIVE_COMPILATION_CONDITIONS": "DEBUG $(inherited)", "SWIFT_OPTIMIZATION_LEVEL": "-Onone"}
RELEASE = {**COMMON, "DEBUG_INFORMATION_FORMAT": "dwarf-with-dsym", "SWIFT_COMPILATION_MODE": "wholemodule",
           "SWIFT_OPTIMIZATION_LEVEL": "-O", "VALIDATE_PRODUCT": "YES"}
APP_SETTINGS = {
    "ASSETCATALOG_COMPILER_APPICON_NAME": "AppIcon",
    "CODE_SIGN_STYLE": "Automatic",
    "CURRENT_PROJECT_VERSION": "$(HQR_BUILD)",
    "FRAMEWORK_SEARCH_PATHS": "$(SRCROOT)/../shared/app/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)",
    "GENERATE_INFOPLIST_FILE": "YES",
    "INFOPLIST_FILE": "HQR/Info.plist",
    "INFOPLIST_KEY_CFBundleDisplayName": "HQR",
    "INFOPLIST_KEY_ITSAppUsesNonExemptEncryption": "NO",
    "INFOPLIST_KEY_LSSupportsOpeningDocumentsInPlace": "YES",
    "INFOPLIST_KEY_UIFileSharingEnabled": "YES",
    "INFOPLIST_KEY_NSLocalNetworkUsageDescription": "HQR reads the comic servers you add on your network (OPDS, Komga, Kavita).",
    "INFOPLIST_KEY_LSApplicationCategoryType": "public.app-category.books",
    "INFOPLIST_KEY_UIApplicationSceneManifest_Generation": "YES",
    "INFOPLIST_KEY_UISupportedInterfaceOrientations_iPad": "UIInterfaceOrientationPortrait UIInterfaceOrientationPortraitUpsideDown UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight",
    "INFOPLIST_KEY_UISupportedInterfaceOrientations_iPhone": "UIInterfaceOrientationPortrait UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight",
    "LD_RUNPATH_SEARCH_PATHS": "$(inherited) @executable_path/Frameworks",
    "MARKETING_VERSION": "$(HQR_VERSION)",
    "OTHER_LDFLAGS": "$(inherited) -framework Shared -lz",
    "PRODUCT_BUNDLE_IDENTIFIER": BUNDLE_ID,
    "PRODUCT_NAME": "$(TARGET_NAME)",
    "SUPPORTS_MACCATALYST": "NO",
}
TEST_SETTINGS = {
    "BUNDLE_LOADER": "$(TEST_HOST)",
    "CODE_SIGN_STYLE": "Automatic",
    "GENERATE_INFOPLIST_FILE": "YES",
    "PRODUCT_BUNDLE_IDENTIFIER": BUNDLE_ID + ".tests",
    "PRODUCT_NAME": "$(TARGET_NAME)",
    "TEST_HOST": "$(BUILT_PRODUCTS_DIR)/HQR.app/$(BUNDLE_EXECUTABLE_FOLDER_PATH)/HQR",
}
UI_SETTINGS = {
    "CODE_SIGN_STYLE": "Automatic",
    "GENERATE_INFOPLIST_FILE": "YES",
    "PRODUCT_BUNDLE_IDENTIFIER": BUNDLE_ID + ".uitests",
    "PRODUCT_NAME": "$(TARGET_NAME)",
    "TEST_TARGET_NAME": APP,
}


def project():
    o = {}
    project_id, main_group, products, config_group = uid("project"), uid("main"), uid("products"), uid("configgroup")
    xcconfig = uid("xcconfig")
    o[xcconfig] = {"isa": "PBXFileReference", "lastKnownFileType": "text.xcconfig", "path": "App.xcconfig", "sourceTree": "<group>"}
    o[config_group] = {"isa": "PBXGroup", "children": [xcconfig], "path": "Config", "sourceTree": "<group>"}

    targets, product_refs, synced = [], [], []
    specs = [
        (APP, "com.apple.product-type.application", "wrapper.application", "app", APP_SETTINGS, []),
        (TESTS, "com.apple.product-type.bundle.unit-test", "wrapper.cfbundle", "xctest", TEST_SETTINGS, [APP]),
        (UI_TESTS, "com.apple.product-type.bundle.ui-testing", "wrapper.cfbundle", "xctest", UI_SETTINGS, [APP]),
    ]
    for name, ptype, ftype, ext, settings, deps in specs:
        target, product = uid("target", name), uid("product", name)
        o[product] = {"isa": "PBXFileReference", "explicitFileType": ftype, "includeInIndex": "0", "path": f"{name}.{ext}", "sourceTree": "BUILT_PRODUCTS_DIR"}
        product_refs.append(product)
        group = uid("synced", name)
        group_obj = {"isa": "PBXFileSystemSynchronizedRootGroup", "path": name, "sourceTree": "<group>"}
        if name == APP:
            exception = uid("exception", name)
            o[exception] = {"isa": "PBXFileSystemSynchronizedBuildFileExceptionSet", "membershipExceptions": ["Info.plist"], "target": target}
            group_obj["exceptions"] = [exception]
        o[group] = group_obj
        synced.append(group)
        phases = []
        if name == APP:
            script = uid("kotlin", name)
            o[script] = {"isa": "PBXShellScriptBuildPhase", "alwaysOutOfDate": "1", "buildActionMask": "2147483647", "files": [],
                         "inputPaths": [], "name": "Kotlin core (Shared framework)", "outputPaths": [], "runOnlyForDeploymentPostprocessing": "0",
                         "shellPath": "/bin/sh", "shellScript": KOTLIN}
            phases.append(script)
        for phase, files in (("PBXSourcesBuildPhase", []), ("PBXFrameworksBuildPhase", []), ("PBXResourcesBuildPhase", [])):
            pid = uid(phase, name)
            o[pid] = {"isa": phase, "buildActionMask": "2147483647", "files": files, "runOnlyForDeploymentPostprocessing": "0"}
            phases.append(pid)
        dependencies = []
        for dep in deps:
            proxy, dependency = uid("proxy", name, dep), uid("dependency", name, dep)
            o[proxy] = {"isa": "PBXContainerItemProxy", "containerPortal": project_id, "proxyType": "1", "remoteGlobalIDString": uid("target", dep), "remoteInfo": dep}
            o[dependency] = {"isa": "PBXTargetDependency", "target": uid("target", dep), "targetProxy": proxy}
            dependencies.append(dependency)
        configs = []
        for config in ("Debug", "Release"):
            cid = uid("config", name, config)
            entry = {"isa": "XCBuildConfiguration", "buildSettings": dict(settings), "name": config}
            if name == APP:
                entry["baseConfigurationReference"] = xcconfig
            o[cid] = entry
            configs.append(cid)
        clist = uid("configlist", name)
        o[clist] = {"isa": "XCConfigurationList", "buildConfigurations": configs, "defaultConfigurationIsVisible": "0", "defaultConfigurationName": "Release"}
        o[target] = {"isa": "PBXNativeTarget", "buildConfigurationList": clist, "buildPhases": phases, "buildRules": [],
                     "dependencies": dependencies, "fileSystemSynchronizedGroups": [group], "name": name,
                     "packageProductDependencies": [], "productName": name, "productReference": product, "productType": ptype}
        targets.append(target)

    o[products] = {"isa": "PBXGroup", "children": product_refs, "name": "Products", "sourceTree": "<group>"}
    o[main_group] = {"isa": "PBXGroup", "children": synced + [config_group, products], "sourceTree": "<group>"}
    pconfigs = []
    for config, base in (("Debug", DEBUG), ("Release", RELEASE)):
        cid = uid("projectconfig", config)
        o[cid] = {"isa": "XCBuildConfiguration", "buildSettings": base, "name": config}
        pconfigs.append(cid)
    plist_id = uid("projectconfiglist")
    o[plist_id] = {"isa": "XCConfigurationList", "buildConfigurations": pconfigs, "defaultConfigurationIsVisible": "0", "defaultConfigurationName": "Release"}
    o[project_id] = {"isa": "PBXProject", "attributes": {"BuildIndependentTargetsInParallel": "1", "LastSwiftUpdateCheck": "2600", "LastUpgradeCheck": "2600"},
                     "buildConfigurationList": plist_id, "developmentRegion": "en", "hasScannedForEncodings": "0",
                     "knownRegions": ["en", "Base", "pt-BR", "es"], "mainGroup": main_group, "minimizedProjectReferenceProxies": "1",
                     "preferredProjectObjectVersion": "77", "productRefGroup": products, "projectDirPath": "", "projectRoot": "", "targets": targets}
    body = "".join(f"\t\t{key} = {plist(value)};\n" for key, value in sorted(o.items()))
    return "// !$*UTF8*$!\n{\n\tarchiveVersion = 1;\n\tclasses = {\n\t};\n\tobjectVersion = 77;\n\tobjects = {\n" + body + "\t};\n\trootObject = " + project_id + ";\n}\n"


def scheme():
    ref = lambda name, ext: (f'<BuildableReference BuildableIdentifier = "primary" BlueprintIdentifier = "{uid("target", name)}" '
                             f'BuildableName = "{name}.{ext}" BlueprintName = "{name}" ReferencedContainer = "container:HQR.xcodeproj"></BuildableReference>')
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<Scheme LastUpgradeVersion = "2600" version = "1.7">
   <BuildAction parallelizeBuildables = "YES" buildImplicitDependencies = "YES">
      <BuildActionEntries>
         <BuildActionEntry buildForTesting = "YES" buildForRunning = "YES" buildForProfiling = "YES" buildForArchiving = "YES" buildForAnalyzing = "YES">
            {ref(APP, "app")}
         </BuildActionEntry>
      </BuildActionEntries>
   </BuildAction>
   <TestAction buildConfiguration = "Debug" selectedDebuggerIdentifier = "Xcode.DebuggerFoundation.Debugger.LLDB" selectedLauncherIdentifier = "Xcode.DebuggerFoundation.Launcher.LLDB" shouldUseLaunchSchemeArgsEnv = "YES">
      <Testables>
         <TestableReference skipped = "NO" parallelizable = "NO">
            {ref(TESTS, "xctest")}
         </TestableReference>
         <TestableReference skipped = "NO" parallelizable = "NO">
            {ref(UI_TESTS, "xctest")}
         </TestableReference>
      </Testables>
   </TestAction>
   <LaunchAction buildConfiguration = "Debug" selectedDebuggerIdentifier = "Xcode.DebuggerFoundation.Debugger.LLDB" selectedLauncherIdentifier = "Xcode.DebuggerFoundation.Launcher.LLDB" launchStyle = "0" useCustomWorkingDirectory = "NO" ignoresPersistentStateOnLaunch = "NO" debugDocumentVersioning = "YES" debugServiceExtension = "internal" allowLocationSimulation = "YES">
      <BuildableProductRunnable runnableDebuggingMode = "0">
         {ref(APP, "app")}
      </BuildableProductRunnable>
   </LaunchAction>
   <ProfileAction buildConfiguration = "Release" shouldUseLaunchSchemeArgsEnv = "YES" savedToolIdentifier = "" useCustomWorkingDirectory = "NO" debugDocumentVersioning = "YES">
      <BuildableProductRunnable runnableDebuggingMode = "0">
         {ref(APP, "app")}
      </BuildableProductRunnable>
   </ProfileAction>
   <AnalyzeAction buildConfiguration = "Debug"></AnalyzeAction>
   <ArchiveAction buildConfiguration = "Release" revealArchiveInOrganizer = "YES"></ArchiveAction>
</Scheme>
"""


def files():
    return {
        PROJECT / "project.pbxproj": project(),
        PROJECT / "xcshareddata/xcschemes/HQR.xcscheme": scheme(),
        PROJECT / "project.xcworkspace/contents.xcworkspacedata": '<?xml version="1.0" encoding="UTF-8"?>\n<Workspace version = "1.0">\n   <FileRef location = "self:"></FileRef>\n</Workspace>\n',
    }


def main():
    wanted = files()
    if "--check" in sys.argv:
        stale = [str(p.relative_to(ROOT)) for p, text in wanted.items() if not p.exists() or p.read_text() != text]
        if stale:
            print("Xcode project out of date: python3 scripts/ios/generate_xcodeproj.py", *stale)
            sys.exit(1)
        print("Xcode project up to date")
        return
    for path, text in wanted.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)
    print("ios/HQR.xcodeproj written")


if __name__ == "__main__":
    main()
