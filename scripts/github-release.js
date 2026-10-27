#!/usr/bin/env node
/**
 * Creates (or updates) the GitHub release for the current app version — tag `v<version>`, titled
 * with the bare version ("0.2.0"), notes = the tagged commit's message unless given.
 *
 * Usage:
 *   yarn release                                   -> release for native/version.properties' version
 *   yarn release --notes "text" | --notes-file path
 *   yarn release --dry-run                         -> show what would happen, touch nothing
 *
 * The routine per commit (see help.md "Versioning & releases"): `yarn bump <minor|patch>` →
 * commit → `git tag -a v<version> -m <version>` → `git push origin develop --follow-tags` →
 * `yarn release`. The tag must already be on GitHub; this script never creates or moves tags.
 *
 * `gh` isn't installed on this machine, so it talks to the REST API directly with the token Git
 * already stores for github.com (`git credential fill`). The token is never printed.
 * 0.x versions and pre-release versions (2.0.0-alpha.1) are marked as pre-releases.
 *
 * No file uploads, on purpose: the user doesn't want APKs on GitHub (2026-10-23). Builds stay local.
 */
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

const known = new Set(['--dry-run', '--notes', '--notes-file']);
for (let i = 0; i < args.length; i++) {
  if (!known.has(args[i])) fail(`unknown argument ${args[i]}\nUsage: yarn release [--notes text | --notes-file path] [--dry-run]`);
  if (args[i] !== '--dry-run') i++;
}

const git = (...gitArgs) => execFileSync('git', gitArgs, { cwd: root, encoding: 'utf8' }).trim();

// Since v2 the version lives in native/version.properties (scripts/bump-version.js).
const properties = fs.readFileSync(path.join(root, 'native', 'version.properties'), 'utf8');
const version = properties.match(/^VERSION_NAME=(.*)$/m)?.[1]?.trim();
if (!version) fail('native/version.properties has no VERSION_NAME');
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

const prerelease = version.startsWith('0.') || version.includes('-');
console.log(`repo:       ${owner}/${repo}`);
console.log(`release:    ${version} (tag ${tag}${prerelease ? ', pre-release' : ''})`);
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

async function api(method, url, body) {
  const res = await fetch(url, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
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

  console.log(release.html_url);
}

main().catch(err => fail(err.cause ? `${err.message} (${err.cause.code ?? err.cause.message})` : err.message));
