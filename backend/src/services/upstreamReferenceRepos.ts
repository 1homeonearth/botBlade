export type UpstreamImportPolicy = "reference-snapshot" | "catalog-source" | "dependency-preferred" | "selective-port";

export type UpstreamReferenceRepo = {
  slug: string;
  label: string;
  repositoryUrl: string;
  uploadedZipName: string;
  targetPath: string;
  rootDirectory: string;
  license: string;
  importPolicy: UpstreamImportPolicy;
};

export const upstreamReferenceRepos: UpstreamReferenceRepo[] = [
  {
    slug: "acode",
    label: "Acode",
    repositoryUrl: "https://github.com/Acode-Foundation/Acode",
    uploadedZipName: "Acode-main.zip",
    targetPath: "vendor/upstream/acode",
    rootDirectory: "Acode-main",
    license: "MIT",
    importPolicy: "selective-port",
  },
  {
    slug: "hackerkid-bots",
    label: "hackerkid/bots",
    repositoryUrl: "https://github.com/hackerkid/bots",
    uploadedZipName: "bots-master.zip",
    targetPath: "vendor/upstream/hackerkid-bots",
    rootDirectory: "bots-master",
    license: "CC0-1.0",
    importPolicy: "catalog-source",
  },
  {
    slug: "jgit",
    label: "JGit",
    repositoryUrl: "https://github.com/eclipse-jgit/jgit",
    uploadedZipName: "jgit-master.zip",
    targetPath: "vendor/upstream/jgit",
    rootDirectory: "jgit-master",
    license: "EDL-1.0",
    importPolicy: "dependency-preferred",
  },
  {
    slug: "fossify-file-manager",
    label: "Fossify File Manager",
    repositoryUrl: "https://github.com/FossifyOrg/File-Manager",
    uploadedZipName: "File-Manager-main.zip",
    targetPath: "vendor/upstream/fossify-file-manager",
    rootDirectory: "File-Manager-main",
    license: "GPL-3.0",
    importPolicy: "reference-snapshot",
  },
];

export function findUpstreamReferenceRepo(slug: string): UpstreamReferenceRepo | undefined {
  return upstreamReferenceRepos.find((repo) => repo.slug === slug);
}

export function getUpstreamReferenceRepoByZip(uploadedZipName: string): UpstreamReferenceRepo | undefined {
  return upstreamReferenceRepos.find((repo) => repo.uploadedZipName === uploadedZipName);
}

export function listReferenceReposByLicense(license: string): UpstreamReferenceRepo[] {
  const normalizedLicense = license.toLowerCase();
  return upstreamReferenceRepos.filter((repo) => repo.license.toLowerCase().includes(normalizedLicense));
}

export function listDirectlyImportableCatalogSources(): UpstreamReferenceRepo[] {
  return upstreamReferenceRepos.filter((repo) => repo.importPolicy === "catalog-source");
}
