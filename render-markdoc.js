const readline = require('readline');
const Markdoc = require('@markdoc/markdoc');
const yaml = require('js-yaml');

let config = {};

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
  terminal: false
});

rl.on('line', (line) => {
  const ast = Markdoc.parse(JSON.parse(line));
  const frontmatter = ast.attributes.frontmatter
    ? yaml.load(ast.attributes.frontmatter)
    : {};
  const html = Markdoc.renderers.html(Markdoc.transform(ast, config));
  const json = JSON.stringify({"html":html,...frontmatter});
  process.stdout.write(json + '\n');
});
