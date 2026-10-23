#!/usr/bin/env node
/**
 * Creates (or updates) the GitHub release for the current app version — tag `v<version>`, titled
 * with the bare version ("0.2.0"), notes = the tagged commit's message unless given.
 *
 * Usage:
 *   yarn release                                   -> release for package.json's version
 *   yarn release --asset android/app/build/outputs/apk/release/DaysCounter-1.0.0.apk
 *   yarn release --notes "text" | --notes-file path
 *   yarn release --dry-run                         -> show what would happen, touch nothing
 *
 * The routine per commit (see help.md "Versioning & releases"): `yarn bump <minor|patch>` →
 * commit → `git tag -a v<version> -m <version>` → `git push origin develop --follow-tags` →
 * `yarn release`. The tag must already be on GitHub; this script never creates or moves tags.
 *
 * `gh` isn't installed on this machine, so it talks to the REST API directly with the token Git
 * already stores for github.com (`git credential fill`). The token is never printed.
 * 0.x versions are marked as pre-releases.
 */
const { Buffer } = require('buffer');
const { execFileSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const args = process.argv.slice(2);
const dryRun = args.includes('--dry-run');

function argValue(name) {
  const i = args.indexOf(name);
  if (i === -1) return null;
  const value = args[i + 1];
  if (!value || value.startsWith('--')) {
    fail(`${name} needs a value`);
  }
  return value;
}

function fail(message) {
  console.error(`release: ${message}`);
  process.exit(1);
}

const known = new Set(['--dry-run', '--asset', '--notes', '--notes-file']);
for (let i = 0; i < args.length; i++) {
  if (!known.has(args[i])) fail(`unknown argument ${args[i]}\nUsage: yarn release [--asset file] [--notes text | --notes-file path] [--dry-run]`);
  if (args[i] !== '--dry-run') i++;
}

const git = (...gitArgs) => execFileSync('git', gitArgs, { cwd: root, encoding: 'utf8' }).trim();

const { version } = JSON.parse(fs.readFileSync(path.join(root, 'package.json'), 'utf8'));
const tag = `v${version}`;

const remote = git('remote', 'get-url', 'origin');
const match = remote.match(/github\.com[:/]([^/]+)\/(.+?)(\.git)?$/);
if (!match) fail(`origin (${remote}) is not a GitHub remote`);
const [, owner, repo] = match;

const notesFile = argValue('--notes-file');
let notes = argValue('--notes') ?? (notesFile ? fs.readFileSync(notesFile, 'utf8') : null);

const remoteTag = git('ls-remote', '--tags', 'origin', `refs/tags/${tag}`);
if (!remoteTag) fail(`tag ${tag} is not on GitHub yet — tag the bump commit and push with --follow-tags first`);
if (notes === null) notes = git('log', '-1', '--format=%B', tag);

const assetPath = argValue('--asset');
if (assetPath && !fs.existsSync(assetPath)) fail(`asset not found: ${assetPath}`);

const prerelease = version.startsWith('0.');
console.log(`repo:       ${owner}/${repo}`);
console.log(`release:    ${version} (tag ${tag}${prerelease ? ', pre-release' : ''})`);
if (assetPath) console.log(`asset:      ${path.basename(assetPath)} (${(fs.statSync(assetPath).size / 1048576).toFixed(1)} MB)`);
if (dryRun) {
  console.log(`\nnotes:\n${notes}\n\n--dry-run: nothing sent.`);
  process.exit(0);
}

function token() {
  const out = execFileSync('git', ['credential', 'fill'], {
    cwd: root,
    input: 'protocol=https\nhost=github.com\n\n',
    encoding: 'utf8',
    env: { ...process.env, GIT_TERMINAL_PROMPT: '0' },
  });
  const found = out.match(/^password=(.+)$/m);
  if (!found) fail('no GitHub credential stored for github.com (git credential fill returned none)');
  return found[1].trim();
}

const auth = token();
const headers = {
  Authorization: `Bearer ${auth}`,
  Accept: 'application/vnd.github+json',
  'X-GitHub-Api-Version': '2022-11-28',
  'User-Agent': 'days-counter-release-script',
};

async function api(method, url, body, extraHeaders = {}) {
  const res = await fetch(url, {
    method,
    headers: { ...headers, ...extraHeaders },
    body: body === undefined ? undefined : Buffer.isBuffer(body) ? body : JSON.stringify(body),
  });
  if (res.status === 404) return null;
  const text = await res.text();
  if (!res.ok) fail(`${method} ${url} → ${res.status} ${text.slice(0, 300)}`);
  return text ? JSON.parse(text) : {};
}

async function main() {
  const base = `https://api.github.com/repos/${owner}/${repo}`;
  const fields = { tag_name: tag, name: version, body: notes, prerelease };
  let release = await api('GET', `${base}/releases/tags/${tag}`);
  if (release) {
    release = await api('PATCH', `${base}/releases/${release.id}`, fields);
    console.log(`updated release ${version}`);
  } else {
    release = await api('POST', `${base}/releases`, fields);
    console.log(`created release ${version}`);
  }

  if (assetPath) {
    const name = path.basename(assetPath);
    const existing = (release.assets || []).find(a => a.name === name);
    if (existing) await api('DELETE', `${base}/releases/assets/${existing.id}`);
    await api(
      'POST',
      `https://uploads.github.com/repos/${owner}/${repo}/releases/${release.id}/assets?name=${encodeURIComponent(name)}`,
      fs.readFileSync(assetPath),
      { 'Content-Type': name.endsWith('.apk') ? 'application/vnd.android.package-archive' : 'application/octet-stream' },
    );
    console.log(`uploaded ${name}`);
  }
  console.log(release.html_url);
}

main().catch(err => fail(err.message));
