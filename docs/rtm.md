# Matriz de rastreabilidade (RTM): HQR

> Gerada por `scripts/docs/build_rtm.py` · Colunas: UC (`docs/use-cases.md`) · Componente e ADR (`docs/architecture.md`) · Tela (`docs/ux-foundations.md`) · FEAT (`docs/implementation-plan.md`) · Teste.

## Requisitos funcionais

| ID | Prior. | UC | Componente / ADR | Tela | FEAT | Teste |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| RF-01 | Must | UC-001, UC-002 | source, ADR-002 | — | FEAT-001 | — |
| RF-02 | Must | UC-012 | ADR-003 | — | FEAT-024 | SourceRegistryTest |
| RF-03 | Should | UC-001, UC-012 | settings | SCR-EXP-001, SCR-SRC-001 | FEAT-025 | GlobalSearchViewModelTest, SourceConfigTest, SourceRegistryTest, SourcesScreensTest, SourcesViewModelTest |
| RF-04 | Must | UC-001, UC-007 | net, ADR-005 | — | FEAT-001 | HqNowSourceTest, MangaDexSourceTest, NetAndCacheTest |
| RF-05 | Must | UC-001, UC-002 | browse | SCR-STA-001 | FEAT-012 | BrowseAndMangaViewModelTest, ContractsTest, GlobalSearchViewModelTest, HqNowSourceTest, MangaDexSourceTest, MangaFlixSourceTest, NetAndCacheTest, ReaderAndHistoryViewModelTest, SearchScreensTest |
| RF-06 | Should | UC-012 | settings | SCR-SRC-002 | FEAT-025 | MangaDexSourceTest, NetAndCacheTest, SourcesScreensTest |
| RF-07 | Must | UC-003 | sources/mangadex, ADR-008 | SCR-OBR-001 | FEAT-002 | NetAndCacheTest |
| RF-08 | Must | UC-012 | sources/mangadex, ADR-008 | SCR-SRC-002 | FEAT-002 | MangaDexSourceTest, NetAndCacheTest, SourcesScreensTest |
| RF-09 | Must | UC-004 | sources/mangadex, ADR-005, ADR-008 | — | FEAT-001 | MangaDexSourceTest |
| RF-10 | Must | UC-012 | sources/mangadex | SCR-SRC-002 | FEAT-001 | — |
| RF-11 | Should | UC-003 | sources/mangadex | SCR-OBR-001 | FEAT-034 | MangaScreensTest |
| RF-12 | Must | UC-001, UC-002 | sources/hqnow | SCR-OBR-001 | FEAT-013 | HqNowSourceTest |
| RF-13 | Must | UC-008 | sources/local, ADR-011, ADR-025 | SCR-SRC-001 | FEAT-014, FEAT-030 | ArchivesAndComicInfoTest, LocalSourceTest |
| RF-14 | Should | UC-008 | sources/local, ADR-011, ADR-025 | SCR-SRC-001 | FEAT-030 | ArchivesAndComicInfoTest |
| RF-15 | Should | UC-008 | sources/local, ADR-025 | SCR-SRC-001 | FEAT-030 | ArchivesAndComicInfoTest, BrowseAndMangaViewModelTest |
| RF-16 | Must | UC-008 | sources/local, ADR-025 | SCR-SRC-001 | FEAT-014 | LocalSourceTest |
| RF-17 | Must | UC-009 | sources/opds | SCR-SRC-001, SCR-SRV-001 | FEAT-015 | ContractsTest, OpdsTest, ServersViewModelTest, SourcesScreensTest |
| RF-18 | Must | UC-009 | sources/opds | SCR-SRV-001, SCR-SRV-002 | FEAT-016 | ContractsTest, OpdsTest, ServersViewModelTest |
| RF-19 | Must | UC-009 | sources/opds | SCR-SRV-001 | FEAT-015 | ContractsTest, OpdsTest, ServersViewModelTest |
| RF-20 | Could | UC-009 | sources/opds | SCR-SRV-002 | FEAT-045 | — |
| RF-21 | Must | UC-002 | browse | SCR-EXP-002 | FEAT-001, FEAT-011, FEAT-062 | BrowseAndMangaViewModelTest, MangaDexSourceTest |
| RF-22 | Must | UC-001 | browse | SCR-EXP-002, SCR-EXP-003 | FEAT-011 | BrowseAndMangaViewModelTest, ContractsTest, MangaDexSourceTest, SearchScreensTest, SourceRegistryTest |
| RF-23 | Must | UC-001 | browse | SCR-SRCH-001 | FEAT-012 | BrowseAndMangaViewModelTest, GlobalSearchViewModelTest, SearchScreensTest |
| RF-24 | Should | UC-001, UC-002 | library, browse | SCR-EXP-002, SCR-EXP-003 | FEAT-026 | BackupServiceTest, FeedScreensTest, FeedViewModelTest, ModelsTest, SearchRepositoryTest |
| RF-25 | Should | UC-001, UC-002 | library, browse, ADR-026 | SCR-EXP-001, SCR-EXP-004 | FEAT-026, FEAT-067 | BackupServiceTest, FeedScreensTest, FeedViewModelTest, SearchRepositoryTest |
| RF-115 | Should | UC-002 | ADR-026 | SCR-EXP-001, SCR-EXP-002 | FEAT-067, FEAT-068 | FeedViewModelTest |
| RF-116 | Should | UC-002 | ADR-026 | SCR-EXP-001 | FEAT-067 | FeedViewModelTest |
| RF-26 | Should | UC-001, UC-002 | browse | SCR-SRCH-001 | FEAT-026 | BrowseAndMangaViewModelTest, GlobalSearchViewModelTest, ModelsTest |
| RF-27 | Could | UC-001 | browse | SCR-SRCH-001 | FEAT-026 | FeedScreensTest, FeedViewModelTest, SearchRepositoryTest |
| RF-28 | Must | UC-001 | browse | SCR-SRCH-001 | FEAT-012 | GlobalSearchViewModelTest, SearchScreensTest |
| RF-29 | Must | UC-003 | browse, ADR-026 | SCR-OBR-001 | FEAT-001, FEAT-065 | BrowseAndMangaViewModelTest, LibraryRepositoriesTest, MangaDexSourceTest |
| RF-30 | Must | UC-003 | browse | SCR-OBR-001 | FEAT-001, FEAT-006 | BrowseAndMangaViewModelTest, MangaDexSourceTest, ReadingRulesTest, SourceRegistryTest |
| RF-31 | Must | UC-003, UC-006 | browse, ADR-026 | SCR-OBR-001 | FEAT-001, FEAT-065 | BrowseAndMangaViewModelTest, ReadingRulesTest |
| RF-32 | Must | UC-003 | browse | SCR-OBR-001 | FEAT-006 | BrowseAndMangaViewModelTest, LibraryRepositoriesTest |
| RF-33 | Must | UC-003 | browse | SCR-OBR-001 | FEAT-006, FEAT-066 | BrowseAndMangaViewModelTest, LibraryRepositoriesTest |
| RF-117 | Must | UC-003 | ADR-026 | SCR-OBR-001 | FEAT-066, FEAT-068 | BrowseAndMangaViewModelTest |
| RF-34 | Should | UC-003 | browse | SCR-OBR-001 | FEAT-034 | MangaScreensTest |
| RF-35 | Could | UC-003 | browse | SCR-OBR-001 | FEAT-047, FEAT-062 | — |
| RF-36 | Must | UC-004 | reader, ADR-007 | SCR-RDR-001, SCR-RDR-002 | FEAT-001, FEAT-003, FEAT-063 | — |
| RF-37 | Should | UC-004 | reader, ADR-007 | SCR-RDR-001, SCR-RDR-002 | FEAT-027 | MangaDexSourceTest, ReaderAndHistoryViewModelTest, ReaderLayoutTest, ReaderSettingsStoreTest |
| RF-38 | Must | UC-004 | reader, ADR-007 | SCR-RDR-001, SCR-RDR-002 | FEAT-001, FEAT-003 | TapZoneMapTest |
| RF-39 | Must | UC-004 | reader, ADR-007 | SCR-RDR-001, SCR-RDR-002 | FEAT-003 | — |
| RF-40 | Must | UC-004 | reader, ADR-007 | SCR-RDR-001 | FEAT-001, FEAT-003 | ReaderAndHistoryViewModelTest |
| RF-41 | Must | UC-004 | images, reader, ADR-007 | SCR-RDR-001 | FEAT-003 | AppImageBytesTest, ReaderAndHistoryViewModelTest |
| RF-42 | Must | UC-004 | reader, ADR-007 | SCR-RDR-003 | FEAT-004, FEAT-063 | ReaderAndHistoryViewModelTest, ReadingRulesTest |
| RF-43 | Must | UC-004 | reader, ADR-007 | SCR-RDR-001 | FEAT-001 | AppScreensTest, LibraryRepositoriesTest, ReaderAndHistoryViewModelTest |
| RF-44 | Should | UC-004 | reader, ADR-007 | SCR-RDR-001, SCR-RDR-002 | FEAT-027 | ReaderLayoutTest, ReaderScreensTest |
| RF-45 | Should | UC-004 | reader, ADR-007 | SCR-RDR-001, SCR-RDR-002 | FEAT-027 | ReaderLayoutTest, ReaderScreensTest |
| RF-46 | Should | UC-004 | reader, ADR-007 | SCR-RDR-001, SCR-RDR-002 | FEAT-027 | ReaderPlatformTest, ReaderSettingsStoreTest |
| RF-47 | Should | UC-004 | reader, ADR-007 | SCR-RDR-001, SCR-RDR-002 | FEAT-027 | ReaderLayoutTest, ReaderSettingsStoreTest |
| RF-48 | Could | UC-004 | reader, ADR-007 | SCR-RDR-001, SCR-RDR-002 | FEAT-043 | — |
| RF-49 | Could | UC-004 | reader, ADR-007 | SCR-RDR-001, SCR-RDR-002 | FEAT-043 | — |
| RF-50 | Could | UC-004 | reader, ADR-007 | SCR-RDR-001, SCR-RDR-002 | FEAT-043 | — |
| RF-51 | Must | UC-004 | reader, ADR-007 | SCR-RDR-001 | FEAT-004 | — |
| RF-97 | Could | UC-004 | reader | SCR-RDR-001 | FEAT-044 | — |
| RF-98 | Must | UC-004 | reader, contentcolor, ADR-021, ADR-026 | SCR-OBR-001, SCR-RDR-001, SCR-RDR-002, SCR-RDR-003, SCR-SET-001 | FEAT-005, FEAT-062, FEAT-065 | BrowseAndMangaViewModelTest, CoverColorTest, LibraryRepositoriesTest |
| RF-52 | Must | UC-005 | library | SCR-LIB-001 | FEAT-007, FEAT-062 | BrowseAndMangaViewModelTest, ContractsTest, GlobalSearchViewModelTest, LibraryRepositoriesTest, LibraryScreensTest, LibraryViewModelTest |
| RF-53 | Must | UC-005 | library | SCR-LIB-001, SCR-LIB-003 | FEAT-008 | ContractsTest, LibraryRepositoriesTest, LibraryScreensTest, LibraryViewModelTest |
| RF-54 | Must | UC-005 | library | SCR-LIB-001, SCR-LIB-002 | FEAT-007 | LibraryRepositoriesTest, LibraryScreensTest, SharedGraphTest |
| RF-55 | Must | UC-005 | library | SCR-LIB-001, SCR-LIB-002 | FEAT-009 | LibraryRulesTest, LibraryViewModelTest |
| RF-56 | Should | UC-005 | library | SCR-LIB-001 | FEAT-028 | LibraryRulesTest, LibraryViewModelTest |
| RF-57 | Should | UC-005 | library | SCR-LIB-001 | FEAT-028 | LibraryViewModelTest |
| RF-58 | Should | UC-005 | library | SCR-LIB-001 | FEAT-028, FEAT-062 | LibraryViewModelTest |
| RF-59 | Must | UC-006 | library | SCR-HIS-001 | FEAT-010, FEAT-064 | LibraryRepositoriesTest, ReaderAndHistoryViewModelTest |
| RF-60 | Must | UC-006 | library | SCR-HIS-001 | FEAT-010 | LibraryRepositoriesTest, ReaderAndHistoryViewModelTest |
| RF-61 | Could | UC-004, UC-006 | library | SCR-HIS-001 | FEAT-047 | — |
| RF-62 | Must | UC-007 | download, ADR-009, ADR-025 | SCR-DL-001 | FEAT-017 | BrowseAndMangaViewModelTest, ContractsTest, DownloadManagerTest, DownloadsScreensTest |
| RF-63 | Must | UC-007 | download, ADR-009, ADR-025 | SCR-DL-001 | FEAT-017 | DownloadManagerTest, DownloadsAndBackupViewModelTest |
| RF-64 | Must | UC-007 | download, ADR-009, ADR-025 | SCR-DL-001 | FEAT-017 | DownloadManagerTest, SharedGraphTest |
| RF-65 | Must | UC-004, UC-007 | download, ADR-009, ADR-025 | SCR-DL-001 | FEAT-018 | DownloadManagerTest |
| RF-66 | Must | UC-007 | download, ADR-009, ADR-025 | SCR-DL-001 | FEAT-018 | DownloadManagerTest, DownloadsAndBackupViewModelTest, DownloadsScreensTest, ReaderAndHistoryViewModelTest |
| RF-67 | Should | UC-007 | download, ADR-009, ADR-025 | SCR-DL-001 | FEAT-029 | DownloadManagerTest, DownloadsAndBackupViewModelTest, DownloadsScreensTest |
| RF-68 | Should | UC-007 | download, ADR-009, ADR-025 | SCR-DL-001 | FEAT-029 | DownloadManagerTest, DownloadsAndBackupViewModelTest, DownloadsScreensTest |
| RF-69 | Should | UC-010 | migration | SCR-OBR-002 | FEAT-031 | MangaScreensTest, MigrationViewModelTest |
| RF-70 | Could | UC-010 | migration | SCR-OBR-002 | FEAT-042 | — |
| RF-71 | Could | UC-010 | library, migration | SCR-OBR-002 | FEAT-042 | — |
| RF-72 | Must | UC-011 | backup, ADR-010 | SCR-SET-002 | FEAT-019 | BackupServiceTest, DownloadsAndBackupViewModelTest, DownloadsScreensTest, OpdsTest |
| RF-73 | Must | UC-003, UC-011 | backup, ADR-010 | SCR-SET-002 | FEAT-019 | BackupServiceTest, ContractsTest, DownloadsAndBackupViewModelTest, DownloadsScreensTest |
| RF-74 | Must | UC-011 | backup, ADR-010 | SCR-SET-002 | FEAT-019 | BackupServiceTest |
| RF-75 | Could | UC-011 | backup, ADR-010 | SCR-SET-002 | FEAT-046, FEAT-064 | ZlibTest |
| RF-76 | Should | UC-012 | settings | SCR-SET-001 | FEAT-033, FEAT-061 | NetAndCacheTest, SettingsScreensTest |
| RF-77 | Must | UC-012 | settings, ADR-017 | SCR-SET-001 | FEAT-020 | SettingsScreensTest |
| RF-78 | Must | UC-012 | app | SCR-SET-003 | FEAT-021 | SettingsScreensTest |
| RF-79 | Could | UC-012 | app | SCR-SET-003 | FEAT-049 | — |
| RF-80 | Must | UC-012 | app | SCR-SET-001 | FEAT-020 | NetAndCacheTest |
| RF-81 | Should | UC-012 | app, ADR-026 | SCR-ONB-001 | FEAT-032, FEAT-065 | NavigationTest |
| RF-82 | Must | UC-013 | updater, ADR-015 | SCR-SET-004 | FEAT-022 | ApkDownloaderTest, ReleaseCheckerTest |
| RF-84 | Must | UC-013 | ADR-015, ADR-016 | SCR-SET-004 | FEAT-022 | ReleaseCheckerTest |
| RF-85 | Could | UC-008 | app | SCR-SRC-001 | FEAT-048 | ModelsTest |
| RF-86 | Must | UC-001, UC-002 | browse | SCR-STA-001 | Fundações | — |
| RF-87 | Must | UC-001 | browse | SCR-STA-001 | FEAT-023, FEAT-061 | BrowseAndMangaViewModelTest |
| RF-88 | Must | UC-004 | app, ADR-004, ADR-018 | SCR-STA-001 | Fundações | BrowseAndMangaViewModelTest, GlobalSearchViewModelTest, LibraryViewModelTest, NavigationTest, ReaderAndHistoryViewModelTest, ServersViewModelTest, SourceRegistryTest |
| RF-89 | Must | UC-011 | backup, ADR-010 | SCR-SET-002 | FEAT-019 | BackupServiceTest, DatabaseFileTest, MigrationTest |
| RF-90 | Must | UC-003 | source, ADR-002 | — | FEAT-001 | LibraryRepositoriesTest, MangaDexSourceTest, SourceRegistryTest |
| RF-91 | Should | UC-012 | source, ADR-002 | — | FEAT-035 | BackupServiceTest, SourceConfigTest |
| RF-92 | Must | UC-003 | browse | SCR-OBR-001 | FEAT-006 | LibraryRepositoriesTest, ReadingRulesTest |
| RF-93 | Could | UC-008 | app | SCR-OBR-001 | FEAT-048 | — |
| RF-94 | Must | UC-007 | app, ADR-009 | SCR-DL-001 | FEAT-017 | — |
| RF-95 | Must | UC-009 | sources/opds | SCR-SRV-001 | FEAT-015 | OpdsTest, SourcesScreensTest |
| RF-96 | Could | UC-009 | sources/opds | SCR-SRV-001 | FEAT-045 | — |
| RF-99 | Should | UC-001 | ADR-024 | SCR-SRC-001 | FEAT-037 | InternetArchiveSourceTest |
| RF-100 | Should | UC-001 | ADR-024 | SCR-SRC-001 | FEAT-038 | NamiComiSourceTest |
| RF-101 | Should | UC-001 | ADR-024 | SCR-SRC-001 | FEAT-039 | MangaFlixSourceTest |
| RF-102 | Should | UC-001 | ADR-024 | SCR-SRC-001 | FEAT-039 | YomuSourceTest |
| RF-103 | Should | UC-001 | ADR-024 | SCR-SRC-001 | FEAT-040 | WeebCentralSourceTest |
| RF-104 | Should | UC-001 | ADR-024 | SCR-SRC-001 | FEAT-041 | XoxoSourceTest |
| RF-105 | Must | UC-012 | sourceconfig, ADR-019 | SCR-SRC-001 | FEAT-024 | SourceConfigTest, SourceRegistryTest, SourcesViewModelTest |
| RF-106 | Should | UC-001 | challenge, ADR-020, ADR-024 | SCR-STA-001 | FEAT-036, FEAT-064 | ChallengeCookiesTest, IosChallengeCookiesTest, SourcesScreensTest, WeebCentralSourceTest |
| RF-107 | Should | UC-014 | ui, web, ADR-004, ADR-022 | SCR-WEB-004 | FEAT-050 | — |
| RF-108 | Should | UC-014 | bridge, ADR-023 | SCR-WEB-001 | FEAT-051 | BridgeTest, WebScreensTest, bridge-worker/index.test.js |
| RF-109 | Should | UC-014 | bridge-worker/, ADR-023 | SCR-WEB-001 | FEAT-051 | BridgeTest, bridge-worker/index.test.js |
| RF-110 | Should | UC-014 | web | SCR-WEB-002 | FEAT-052 | WebMessagesTest |
| RF-111 | Should | UC-014 | web, ADR-023 | SCR-SRV-001 | FEAT-053 | BridgeTest, WebMessagesTest |
| RF-112 | Should | UC-014 | web, ADR-022 | SCR-WEB-003 | FEAT-054 | ReaderAndHistoryViewModelTest, WebScreensTest |
| RF-113 | Should | UC-014 | web, ADR-022 | SCR-RDR-001 | FEAT-050 | — |
| RF-114 | Should | UC-014 | web, ADR-023 | SCR-SRC-001, SCR-WEB-004 | FEAT-050 | WebScreensTest |

## Requisitos não funcionais

| ID | UC | ADR | Verificação | FEAT |
| :-- | :-- | :-- | :-- | :-- |
| RNF-01 | transversal | — | — | — |
| RNF-02 | UC-004 | ADR-006 | — | — |
| RNF-03 | UC-004 | ADR-006 | — | — |
| RNF-04 | UC-004 | ADR-006 | — | — |
| RNF-05 | UC-001 | — | — | — |
| RNF-06 | UC-005 | — | — | — |
| RNF-07 | transversal | ADR-014 | — | — |
| RNF-08 | transversal | — | — | — |
| RNF-09 | UC-009 | — | — | — |
| RNF-10 | UC-012 | ADR-014 | — | — |
| RNF-11 | UC-004 | ADR-021 | — | — |
| RNF-12 | transversal | ADR-012 | — | — |
| RNF-13 | transversal | — | — | — |
| RNF-14 | UC-012 | ADR-017 | — | — |
| RNF-15 | transversal | ADR-001 | — | — |
| RNF-16 | transversal | ADR-003 | — | — |
| RNF-17 | UC-003 | — | — | — |
| RNF-18 | transversal | ADR-004 | — | — |
| RNF-19 | transversal | ADR-014 | — | — |
| RNF-20 | transversal | — | — | — |
| RNF-21 | UC-007 | — | — | — |
| RNF-22 | UC-007 | — | — | — |
| RNF-23 | UC-007 | ADR-006 | — | — |
| RNF-24 | UC-007 | ADR-005 | — | — |
| RNF-25 | UC-007 | ADR-004 | — | — |
| RNF-26 | UC-007, UC-008 | ADR-011 | — | — |
| RNF-27 | UC-007 | ADR-009 | — | — |
| RNF-28 | UC-007 | ADR-009, ADR-025 | — | — |
| RNF-29 | UC-012 | — | — | — |
| RNF-30 | UC-014 | ADR-022 | — | — |
| RNF-31 | UC-014 | ADR-022 | — | — |
| RNF-32 | UC-014 | ADR-023 | — | — |
| RNF-33 | UC-014 | — | — | — |
