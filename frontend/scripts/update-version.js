const fs = require('fs');
const path = require('path');

// Read version from package.json
const packageJsonPath = path.join(__dirname, '..', 'package.json');
const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
const version = packageJson.version;

console.log(`Updating version to: ${version}`);

// Update the version file
const versionContent = `// This file is auto-generated during build/deployment
// Update this version number when deploying new versions
export const APP_VERSION = '${version}';
`;

const versionFilePath = path.join(__dirname, '..', 'src', 'app', 'version.ts');
fs.writeFileSync(versionFilePath, versionContent);

console.log(`Version file updated: ${versionFilePath}`);
console.log('Ready for deployment!'); 