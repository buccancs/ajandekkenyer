# Thesis Documentation

This directory contains the complete thesis documentation for the Multi-Sensor Recording System for Contactless GSR Prediction Research project, including both source chapters and build infrastructure for LaTeX compilation.

## Directory Structure

### Source Content
- **[`final/`](final/README.md)** - Final thesis chapters and appendices in Markdown and LaTeX formats
- **[`final/latex/`](final/latex/README.md)** - LaTeX compilation sources and build files
- **[`references.md`](final/references.md)** - Bibliography and references

### Supporting Documents
- **[`cs_project_marking_form_meng_1819.md`](cs_project_marking_form_meng_1819.md)** - MEng project evaluation criteria
- **[`evaluation_comparison_report.md`](evaluation_comparison_report.md)** - Evaluation methodology comparison
- **[`structure_full.md`](structure_full.md)** - Complete thesis structure overview
- **[`structure_lean.md`](structure_lean.md)** - Lean thesis structure alternative

## LaTeX Build System

### Master Build File
- **Master file**: `docs/thesis_report/final/latex/main.tex`
- **Bibliography**: `../../../../references.bib` (root level shared references)

### Available Chapters
- **Chapter 1**: Introduction (`final/1.md` / `final/latex/1.tex`)
- **Chapter 2**: Background and Research Context (`final/2.md` / `final/latex/chapter2.tex`)
- **Chapter 3**: Requirements Analysis (`final/3.md` / `final/latex/3.tex`)
- **Chapter 4**: System Design and Architecture (`final/4.md` / `final/latex/chapter4.tex`)
- **Chapter 5**: Evaluation and Testing (`final/5.md` / `final/latex/chapter5.tex`)
- **Chapter 6**: Conclusion and Future Work (`final/6.md` / `final/latex/6.tex`)

### Appendices
Complete set of appendices covering system documentation, user manuals, test reports, and supporting materials. See [final/README.md](final/README.md) for detailed contents.

## Quick Build Instructions

### Prerequisites
- LaTeX distribution (TeX Live/MiKTeX)
- BibTeX for bibliography management
- Required packages: `graphicx`, `amsmath`, `amssymb`, `siunitx`, `hyperref`, `natbib`
- For Mermaid diagrams: Node.js 16+ and Mermaid CLI (`npm install -g @mermaid-js/mermaid-cli`)

### Build Process
1. **Navigate to build directory**:
   ```bash
   cd docs/thesis_report/final/latex
   ```

2. **Compile thesis**:
   ```bash
   pdflatex -shell-escape main.tex  # Enable shell-escape for Mermaid
   bibtex main                      # For bibliography
   pdflatex -shell-escape main.tex  # Update references
   pdflatex -shell-escape main.tex  # Final compilation
   ```

3. **Output**: `main.pdf` contains the complete thesis

### Build Notes
- Bibliography references are resolved from `../../../../references.bib`
- Shell-escape flag (`-shell-escape`) required for Mermaid diagram rendering
- Some figure assets may require creation or commenting out during development

## Current Status

### Completed Conversions
- [x] Chapter 1: Introduction (LaTeX format)
- [x] Chapter 2: Background and Research Context (LaTeX format)
- [x] Chapter 3: Requirements Analysis (LaTeX format)  
- [x] Chapter 4: System Design and Architecture (LaTeX format)
- [x] Chapter 5: Evaluation and Testing (LaTeX format)
- [x] Chapter 6: Conclusion and Future Work (LaTeX format)
- [x] Appendices A-Z: Complete appendix set (LaTeX format)

### Ongoing Tasks
- [ ] Citation mapping: Replace numeric citations `[n]` with proper `\cite{...}` keys
- [ ] Symbol validation: Ensure `\texttimes`/`\textmu` render correctly
- [ ] Figure assets: Provide missing figure files or placeholders
- [ ] LaTeX refinement: Convert verbatim blocks to proper LaTeX structures

## Development Workflow

### Adding New Content
1. Create/edit Markdown source in `final/`
2. Convert to LaTeX format in `final/latex/`
3. Update master file includes in `main.tex`
4. Test compilation and resolve any build errors

### Quality Assurance
- Use content comparison tools in `final/` to verify Markdown-LaTeX synchronisation
- Validate bibliography entries and citation format
- Check figure references and cross-references
- Ensure academic writing standards compliance

## Mermaid Diagrams

### LaTeX Integration
Direct Mermaid rendering in LaTeX using the `mermaid` package:

**Build Requirements:**
- LaTeX: Enable shell-escape when compiling (`-shell-escape` flag)
- Node.js 16+ installed and available on PATH
- Mermaid CLI: `npm install -g @mermaid-js/mermaid-cli`

**Usage Example:**
```latex
\begin{mermaid}
graph TD
    A[Start] --> B[Process]
    B --> C[End]
\end{mermaid}
```

### Fallback Strategy
If Mermaid compilation fails:
1. Generate static images externally using `mmdc` command
2. Include as standard LaTeX figures with `\includegraphics`
3. Comment out live Mermaid blocks for debugging

## Troubleshooting

### Common Issues
- **Missing bibliography**: Ensure `references.bib` path is correct
- **Figure not found**: Check figure file paths or comment out for testing
- **Mermaid errors**: Verify Node.js and mmdc installation
- **Package not found**: Install missing LaTeX packages
- **Citation errors**: Verify BibTeX keys match bibliography entries

### Build Exclusions
- Do not include `external/` or `docs/generated_docs` in build artifacts
- Exclude temporary LaTeX files (`.aux`, `.log`, `.toc`) from version control
