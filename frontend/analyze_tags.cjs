const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, 'src', 'views', 'ChatView.vue');
const content = fs.readFileSync(filePath, 'utf-8');
const lines = content.split('\n');

// Find first <template> (bare, no v-if/v-else) and last </template> before <script>
let templateStart = -1, templateEnd = -1;
for (let i = 0; i < lines.length; i++) {
  const line = lines[i];
  if (/^\s*<template\s*>\s*$/.test(line) && templateStart === -1) templateStart = i;
  // Find the </template> that is right before <script setup>
  if (/^\s*<\/template>\s*$/.test(line)) {
    // Check if next non-blank line starts with <script
    let nextNonBlank = -1;
    for (let j = i + 1; j < lines.length; j++) {
      if (lines[j].trim().length > 0) { nextNonBlank = j; break; }
    }
    if (nextNonBlank >= 0 && /^\s*<script/.test(lines[nextNonBlank])) {
      templateEnd = i;
      break;
    }
  }
}

console.log(`Template boundaries: line ${templateStart + 1} to ${templateEnd + 1}\n`);

// Stack-based tag analysis
const stack = [];
const warnings = [];
const allOpens = []; // track all opening positions for debugging

function analyzeLine(line, lineNum) {
  const clean = line.replace(/<!--[\s\S]*?-->/g, '');

  const openDiv = /<div\b(?![^>]*\/\s*>)(?=[^>]*>)/g;
  const openSpan = /<span\b(?![^>]*\/\s*>)(?=[^>]*>)/g;
  const closeDiv = /<\/div\s*>/g;
  const closeSpan = /<\/span\s*>/g;

  const tags = [];
  let m;

  while ((m = openDiv.exec(clean)) !== null) tags.push({ type: 'open', tag: 'div', pos: m.index });
  while ((m = openSpan.exec(clean)) !== null) tags.push({ type: 'open', tag: 'span', pos: m.index });
  while ((m = closeDiv.exec(clean)) !== null) tags.push({ type: 'close', tag: 'div', pos: m.index });
  while ((m = closeSpan.exec(clean)) !== null) tags.push({ type: 'close', tag: 'span', pos: m.index });

  tags.sort((a, b) => a.pos - b.pos);

  for (const t of tags) {
    if (t.type === 'open') {
      stack.push({ tag: t.tag, line: lineNum, text: line.trim().substring(0, 60) });
      allOpens.push({ ...stack[stack.length - 1], index: stack.length - 1 });
    } else {
      // Find matching open from right
      let found = -1;
      for (let j = stack.length - 1; j >= 0; j--) {
        if (stack[j].tag === t.tag) { found = j; break; }
      }
      if (found >= 0) {
        stack.splice(found, 1);
      } else {
        warnings.push(`Unexpected </${t.tag}> at line ${lineNum}`);
      }
    }
  }
}

for (let i = templateStart; i <= templateEnd; i++) {
  analyzeLine(lines[i], i + 1);
}

console.log('Warnings:');
if (warnings.length === 0) {
  console.log('  (none)');
} else {
  warnings.forEach(w => console.log(`  ${w}`));
}

console.log('\n=== Final Stack (unclosed tags) ===\n');
if (stack.length === 0) {
  console.log('All tags properly closed.');
} else {
  const divs = stack.filter(s => s.tag === 'div');
  const spans = stack.filter(s => s.tag === 'span');
  console.log(`Unclosed <div>: ${divs.length}`);
  divs.forEach((s, i) => console.log(`  ${i+1}. line ${s.line}: ${s.text}`));
  console.log(`\nUnclosed <span>: ${spans.length}`);
  spans.forEach((s, i) => console.log(`  ${i+1}. line ${s.line}: ${s.text}`));
}
