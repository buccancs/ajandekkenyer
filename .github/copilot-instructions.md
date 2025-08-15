# Academic Writing and Documentation Guidelines for the **Multi‑Sensor Recording System for Contactless GSR Prediction Research** Project

These guidelines apply to **all project documentation** -- including the
MEng thesis/dissertation chapters, technical design documents,
repository README files, Architecture Decision Records (ADRs), and
research methodology reports. The goal is to ensure every document
conforms to UCL's academic standards for MEng projects in terms of tone,
structure, referencing, ethics, and clarity. Adhering to these
guidelines will produce professional, consistent documentation that
meets academic requirements and supports the reproducibility and
integrity of the research.

Only use ASCII-safe characters.

## Testing and Evaluation
- **NEVER USE FAKE DATA/LOGS OR MOCK DATA/LOGS**
- **Testing:** The system must be tested in a controlled environment
  (e.g. lab, field, etc.) to ensure it meets the requirements. This
  includes **unit tests**, **integration tests**, and **system tests**
  (e.g. performance, usability, etc.).
- **Evaluation:** The system must be evaluated in a controlled
  environment to ensure it meets the requirements. This includes
  **quantitative evaluation** (e.g. accuracy, reliability,
  performance, etc.) and **qualitative evaluation** (e.g. usability,
  user experience, etc.).
- **Reproducibility:** The system must be reproducible to ensure that

## Scope and Applicability

- **Thesis/Dissertation:** The final report of the project must follow
  UCL's academic conventions for structure and style. This includes all
  chapters (from Introduction to Conclusion) and any appendices or
  supplementary materials.
- **Technical Documentation:** Documents describing system architecture,
  design decisions, and implementation (e.g. system manuals, developer
  guides) should be written with the same formality and clarity as the
  thesis. They should complement the thesis by providing detailed
  technical reference without duplicating content.
- **Repository Materials (README and Code Docs):** The code repository's
  README and any in-code documentation or comments should also reflect a
  professional tone. They should explain how to build/use the system in
  clear, concise language, and link to relevant sections of the thesis
  or technical docs for further detail.
- **ADRs (Architecture Decision Records):** ADRs recording key design
  decisions must be maintained in a clear, structured format (context,
  options, decision, rationale). These records should be referenced in
  the thesis or technical report where those decisions are discussed,
  ensuring transparency in the design process.
- **Methodology Reports and Others:** Any interim reports (e.g.
  proposal, literature survey, methodology write-up) should likewise
  follow these style and referencing guidelines so that they can be
  integrated into the final dissertation with minimal revision.

## Academic Writing Style

All documentation should be written in a **formal, scholarly style**
consistent with UCL's expectations:

- **Objective Tone:** Maintain an **objective and professional tone**
  throughout. Avoid emotive or casual language. The writing should
  **focus on the research, data, and findings**, not personal opinions.
- **Third-Person Perspective:** **Avoid first-person pronouns** ("I",
  "we", "my") in formal project
  documents[\[1\]](https://www.sjsu.edu/writingcenter/docs/handouts/First%20Person%20Usage%20in%20Academic%20Writing.pdf#:~:text=In%20most%20academic%20writing%2C%20first,is%20a%20lab%20report%2C%20the).
  Instead of writing "I developed a mobile application," use third
  person or passive constructions, e.g. "A mobile application was
  developed as part of this project," or better, "The project includes a
  mobile application component." This depersonalizes the text and keeps
  the focus on the work itself, which is preferred in academic
  writing[\[1\]](https://www.sjsu.edu/writingcenter/docs/handouts/First%20Person%20Usage%20in%20Academic%20Writing.pdf#:~:text=In%20most%20academic%20writing%2C%20first,is%20a%20lab%20report%2C%20the).
- **Active Voice and Clarity:** Prefer the **active voice** for clarity
  and directness, but without using "I/we". For example, write "The
  system design addresses the need for synchronisation" rather than "The
  need for synchronisation is addressed by the system design." Active
  voice makes explanations clearer and more concise. However, if
  avoiding first-person makes active voice awkward, a neutral
  construction is acceptable. Strive for **clear, concise sentences**,
  generally targeting \~20--25 words per sentence for readability.
- **Consistent Terminology:** Use **precise technical terms** and use
  them consistently throughout all documents. For instance, if using the
  term "Galvanic Skin Response (GSR)", define it on first use and then
  use the same abbreviation thereafter. Avoid switching between synonyms
  or different abbreviations for the same concept. Consistency in
  terminology improves clarity and avoids confusing the reader.
- **No Contractions or Slang:** Write in full, formal English. **Do not
  use contractions** ("don't" → "do not") or colloquial expressions.
  Ensure spelling follows British English standards (e.g. "behaviour"
  not "behaviour", "optimisation" not "optimisation") for consistency.
- **Grammatical Precision:** Ensure grammar and punctuation are correct.
  Use tools or proofreading to eliminate errors. Each sentence should be
  complete and unambiguous. Avoid run-on sentences and overly complex
  structures -- clarity is more important than literary flourish in
  technical writing.
- **Paragraph Structure:** Organise writing into **coherent
  paragraphs**, each focusing on a single idea or topic. Typically, a
  paragraph in the thesis or report should be \~3--5 sentences long. Use
  **logical transitions** between paragraphs to maintain a smooth flow
  of ideas. The overall narrative should progress in a logical manner,
  guiding the reader from background material into methods, results, and
  conclusions seamlessly.

## Document Structure and Content Organisation

Each document should have a **clear, logical structure** that meets
academic and project requirements. The **MEng thesis** in particular
must follow a conventional structure as outlined in the UCL project
handbook (with appropriate adjustments for this specific project):

- **Introduction (Chapter 1):** Provides broad context and motivation
  for the project, narrowing down to the specific problem addressed.
  Clearly state the **research problem, aim, and objectives** in this
  section[\[2\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L5-L13).
  Also outline the scope of the work and an overview of the thesis
  structure (how subsequent chapters are organised).

- **Literature Review / Background (Chapter 2):** A comprehensive review
  of relevant literature, theories, and existing technologies. This
  chapter should demonstrate an understanding of prior work on
  **contactless GSR measurement, emotion/stress detection, multisensor
  systems**, etc. Critically compare approaches, and identify the
  **gap** that this project is addressing. Academic sources (journals,
  conference papers) must be cited heavily here to ground the project in
  existing
  knowledge[\[3\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L15-L23).

- **Requirements and Analysis (Chapter 3):** Define the **engineering
  requirements** for the system based on the problem analysis. This may
  include functional requirements (e.g. number of sensors,
  synchronisation precision, data types) and non-functional requirements
  (e.g. usability,
  reliability)[\[4\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L80-L88)[\[5\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L90-L98).
  Include any analysis of alternatives or design considerations, and
  justify choices with evidence (references to literature or standards
  if applicable).

- **Design and Methodology (Chapter 4):** Present the **system
  architecture and design** of the solution in
  detail[\[6\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L118-L126).
  Use subsections to cover different aspects of the design (e.g.
  hardware design, software architecture, communication protocols, data
  processing
  pipeline)[\[7\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L134-L143)[\[8\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L152-L160).
  Incorporate **architecture diagrams** to illustrate the system
  structure. Also describe the **methodological approach** -- how you
  plan to validate the system, any experimental design, and any specific
  engineering methods or algorithms developed. If Architecture Decision
  Records were used, summarise key decisions here (and reference the
  full ADRs).

- **Implementation (could be part of Design or separate Chapter 5):**
  Provide technical details of how the design was realised. This can
  include descriptions of the **hardware components** (sensors, devices
  used), the **software implementation** (e.g. Android app development,
  Python desktop application, libraries and frameworks used), and any
  **calibration or configuration** steps. Where relevant, refer to code
  modules or classes by name to tie the write-up to the actual
  implementation. Ensure this section remains a high-level explanation
  -- lengthy code listings can be moved to an appendix (with references
  in the text).

- **Testing and Evaluation (Chapter 5):** Document how the system was
  tested and
  evaluated[\[9\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L222-L230)[\[10\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L246-L254).
  Describe the **testing strategy** (unit testing, integration testing,
  system
  testing)[\[11\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L230-L238)
  and any simulation or tooling used to generate test data. Present the
  **results** of experiments or performance evaluations in a clear
  manner (graphs, tables, metrics), and narrate the findings. For
  example, if measuring GSR data quality or synchronisation accuracy,
  report those quantitative results here. Include analysis of whether
  requirements were met and discuss any observed limitations. This
  chapter should also cover **results analysis and discussion** --
  interpret what the results mean for the research questions (sometimes
  this is split into a separate Discussion chapter, but it can be
  combined with Evaluation in an engineering thesis for brevity).

- **Discussion (if separate, Chapter 6):** Critically **discuss the
  outcomes**. Compare results to the expectations or to literature.
  Explain any discrepancies or surprising findings. Discuss the
  **implications of the work**: how does this system advance the field
  of GSR measurement or what are its practical limitations? You may also
  discuss any trade-offs in the design and how they were addressed.

- **Conclusions (Final Chapter):** Summarise the project's achievements
  and **how the objectives were
  met**[\[12\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L266-L274).
  State clearly the **contributions** of the work (e.g. development of a
  novel multisensor system, new synchronisation algorithm, etc.).
  Include a frank statement of **limitations** of the project and
  suggestions for **future
  work**[\[13\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L298-L306).
  The conclusion should give the reader a clear sense of what was
  accomplished and what could follow.

- **References:** A complete reference list of all sources cited in the
  text, formatted in the chosen citation style (Harvard or IEEE). Every
  citation in the text must have a corresponding entry here, and vice
  versa. Ensure the bibliography is formatted consistently and contains
  all required information for each source.

- **Appendices:** Use appendices for supplementary material that is too
  detailed for the main body but is important for completeness. For
  example:

- *User Manual or System Manual:* Detailed instructions on setting up
  and using the system, which may be summarised in the main text but
  given in full in an
  appendix[\[14\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L310-L318).

- *Technical Schematics or Code Listings:* Large diagrams, hardware
  schematics, or snippets of code (or algorithm pseudo-code) can be
  placed here so as not to break the flow of the main
  text[\[15\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L354-L361).
  Each appendix should be labelled (Appendix A, B, etc.) and referred to
  from the main chapters when relevant.

- *Raw Data or Test Logs:* If applicable, include samples of raw data,
  calibration curves, or extended test results to support claims made in
  the Evaluation
  chapter[\[16\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L340-L348).
  This supports transparency and reproducibility.

- *Project Management Artefacts:* Optionally, an appendix can include
  the project Gantt chart, risk assessment tables, meeting logs, or
  other management documents for reference.

- **Other Documentation:** Beyond the thesis, ensure **other documents**
  are well-structured too:

- *README.md:* at a minimum, introduce the project, list
  software/hardware requirements, provide setup/build instructions, and
  usage examples. Use clear headings (Installation, Usage, etc.) and
  keep it concise. This can be less formal in tone than the thesis but
  should remain professional and free of slang. If the README is public,
  it should not reveal confidential details but should still align with
  the thesis content.

- *Technical Design Documents:* follow a logical structure (e.g.,
  Introduction, Architecture Overview, Component Details, etc.). Align
  these with the thesis design chapter, expanding on details as needed.
  If using a modular documentation site or wiki, organise sections so
  that readers can find information easily (consider a table of contents
  or hyperlinks between sections).

- *ADRs:* Each ADR should be a short, stand-alone document following a
  consistent template (Title, Status, Context, Decision, Consequences).
  Number them (ADR-001, ADR-002, \...) and store them in the repository.
  In the thesis or main docs, you might not reproduce full ADR text, but
  refer to them by ADR number when you mention major decisions (e.g.,
  "The choice of BLE protocol was made early (see ADR-003) to ensure low
  power consumption..."). This practice ties your narrative to
  documented decisions.

By following a well-defined structure in every document, you ensure the
**content is organised and accessible**. A reader (or examiner) should
be able to navigate your thesis or documentation easily via the headings
and subheadings. Using a consistent structure across documents also
reinforces the modular documentation approach (each piece has its place
and purpose).

## Modular Documentation and Cross-Referencing

Adopt a **modular documentation approach** to manage the breadth of
project documentation. Modular documentation means each document or
section has a clear focus, and documents reference each other to avoid
duplication:

- **Separation of Concerns:** Divide documentation into modules or
  separate files by topic. For example, keep the user guide separate
  from the technical architecture description, and keep detailed design
  decisions in ADRs. This **"bite-sized" modular approach encourages
  consistency and content
  reuse**[\[17\]](https://opensource.com/article/17/9/modular-documentation#:~:text=To%20make%20writing%20user%20story,fundamental%20content%20types%20provides%C2%A0several%20advantages),
  and makes maintenance easier. Writers (or contributors) can update one
  module without affecting the entire documentation set, and content can
  be repurposed in different contexts.
- **Avoid Redundancy:** Do not duplicate large sections of text across
  the thesis, README, and technical docs. Instead, **cross-reference**
  where appropriate. For instance, the thesis might contain a summary of
  the system architecture with a figure, while a detailed breakdown of
  each module is given in a separate design document or appendix. In the
  thesis text you can refer the reader to that document/appendix for
  more information. This ensures there is a **single source of truth**
  for each aspect of the project, which reduces inconsistency and
  errors.
- **Cross-Referencing Practice:** Utilise cross-references within and
  between documents. In the thesis, use section references ("see Chapter
  4 for design details") or appendix references ("see Appendix B for the
  full user manual"). In the repository docs, provide links to thesis
  sections or published paper (if any) for theoretical background. This
  interconnected approach helps readers navigate to the level of detail
  they need without losing the thread of the main narrative.
- **Linking to Code and Implementation:** Ensure that documentation is
  tightly **linked to the actual code and system**:
- When describing the software architecture in writing, mention module
  or class names as implemented in the code (for example, "the
  `DataSyncService` module handles timestamp coordination"). This gives
  the reader a hook to find the implementation in the repository if
  needed.
- Include small **code snippets or examples** in the documentation
  (formatted in code blocks) to illustrate key algorithms or data
  structures, but only if they help understanding. Longer code excerpts
  should go in an appendix or be referenced via a link to the repository
  file.
- If the project includes diagrams (UML diagrams, system block
  diagrams), ensure they reflect the actual code structure and are
  updated when the code changes. Each figure should be labelled and
  referred to in the text for explanation.
- Keep the documentation updated alongside code changes. If a feature is
  modified or a new version of hardware is used, update the relevant
  documents immediately. This traceability prevents divergence where the
  documentation no longer matches the system.
- **Architecture References:** The documentation should clearly explain
  the system's **architecture and design decisions**. Use architectural
  diagrams and describe them in the text. Additionally, refer to
  **industry standards or patterns** when relevant (e.g., if using an
  MVC software architecture or a publish-subscribe pattern, mention it
  and cite sources or standard definitions for it). This shows that the
  design is informed by established practices. When you have formal
  architecture descriptions (like in ADRs), incorporate their outcomes
  into the narrative: for instance, "Given the requirement for reliable
  data streaming, the system adopts a client-server architecture over
  BLE as documented in ADR-002."
- **Template and Consistency:** If multiple people or tools contribute
  to documentation, consider using templates for consistency. For
  example, all ADRs follow one template, all lab reports follow another.
  Consistent formatting and style across modules make the documentation
  feel
  cohesive[\[18\]](https://opensource.com/article/17/9/modular-documentation#:~:text=,overhead%20of%20formatting%20and%20structuring).
  While templates help with structure, **author oversight is still
  required to ensure technical accuracy and appropriate tone in each
  module**[\[19\]](https://opensource.com/article/17/9/modular-documentation#:~:text=overhead%20of%20formatting%20and%20structuring).

By writing documentation in modular pieces and linking them, you
**simplify maintenance and improve user navigation**. Both writers and
readers benefit: writers can focus on one aspect at a time without
forgetting the big picture, and readers can get targeted information
without wading through irrelevant
details[\[20\]](https://opensource.com/article/17/9/modular-documentation#:~:text=,less%20daunting%20for%20new%20colleagues)[\[18\]](https://opensource.com/article/17/9/modular-documentation#:~:text=,overhead%20of%20formatting%20and%20structuring).
This approach aligns with modern documentation best practices and will
help keep the project documents well-organised and consistent over time.

## Referencing and Citation Standards

Robust referencing is a critical part of academic writing and applies to
all project documents (especially the thesis, but also any report or
paper). The goal is to **give credit to existing work, situate your
project in the context of prior research, and uphold academic honesty**.
Key guidelines include:

- **Use an Approved Citation Style:** UCL does not enforce a single
  referencing style across all
  departments[\[21\]](https://subjectguides.york.ac.uk/referencing-style-guides/ieee#:~:text=IEEE%20,in%20electronics%20and%20related%20disciplines),
  but engineering projects typically use either **Harvard
  (author-date)** or **IEEE (numeric)** style. Confirm the preferred
  style with your department/supervisor, then **use it consistently**
  throughout all documents. This means in-text citations and reference
  list entries should follow the same format uniformly. For example:
- *Harvard style:* in-text cite as (Author, Year) with an alphabetical
  reference list.
- *IEEE style:* in-text cite with numbers in square brackets \[1\] with
  a numbered reference list in order of appearance.
- **Cite Academic Literature for All Key Statements:** **Support claims
  with evidence and
  citations[\[22\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/module_deep_dive/test_academic_document.md#L4-L11)[\[23\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/module_deep_dive/test_academic_document.md#L6-L8).**
  Whenever you state a fact, theory, or comparison that is derived from
  another source, provide a citation. This includes background facts
  (e.g., physiology of GSR), related work (previous studies on stress
  measurement), and any specific algorithms or techniques adopted from
  literature. Citations **give credit to the original authors and allow
  readers to locate the sources** of your
  information[\[24\]](https://studentsunionucl.org/academic-writing-guide#:~:text=Generally%20UCL%20uses%20either%20Harvard,your%20argument%20with%20other%20works).
  They also demonstrate the breadth of your research. *As a rule of
  thumb, if a piece of information is not common knowledge or entirely
  your own finding, cite a source for it.*
- **Reference Technical Standards and Documentation:** In an engineering
  project, you may rely on **technical standards (IEEE, ISO, etc.)** or
  manufacturer datasheets and documentation. These should be referenced
  just like academic papers:
- If you discuss Bluetooth Low Energy communication, cite the relevant
  Bluetooth SIG specification or IEEE standard.
- If you use an ISO standard for data security or an IEEE standard for
  sensor calibration, reference the official standard publication (with
  standard number and year).
- Datasheets for sensors (e.g., the Shimmer GSR sensor specifications)
  or API documentation for libraries can be cited or at least referenced
  (possibly as footnotes or endnotes if not in the main reference list).
  For instance, "The sensor's input range is ±4 μS (Shimmer3 GSR+
  datasheet[\[25\]](https://sruc.libguides.com/c.php?g=250237&p=4952318#:~:text=Referencing%20,UCL%20Institute%20of%20Education))"
  -- where the datasheet is listed in references.
- **Citation of Tools and Libraries:** If your project uses specific
  software libraries, frameworks, or tools that are significant to the
  methodology, mention and cite their official documentation or a
  relevant paper. For example, if using a machine learning library or a
  statistical tool, cite the user guide or a publication about it. This
  not only gives credit but also helps readers know what external
  resources were used. In IEEE style you might cite it as a reference,
  in Harvard you might just mention the name and perhaps a URL or
  footnote.
- **Format the Reference List Properly:** The **reference list
  (bibliography)** should contain full details for every source cited.
  Follow all formatting rules of the chosen style meticulously (author
  names, title, journal or book name, volume/issue, page numbers, year,
  publisher, etc., as required by the style). Use a consistent font and
  spacing for the reference list and consider using a reference
  management tool (EndNote, Zotero, BibTeX in LaTeX, etc.) to maintain
  consistency. In IEEE style, references are numbered in order and
  listed by that number; in Harvard, order alphabetically by author
  surname. *Accuracy in referencing is crucial* -- examiners will notice
  errors in citations or bibliography formatting.
- **In-text Citation Clarity:** Ensure citations are placed
  appropriately in the text. In Harvard style, a citation can be written
  as part of the narrative ("According to Smith (2020)...") or in
  parentheses (Smith, 2020) after the statement. In IEEE, just use the
  bracketed number at the end of the sentence or clause. Punctuation
  should come *after* the citation for clarity. Example: "GSR is a
  widely used indicator of sympathetic nervous activity (Smith et
  al., 2018) and is often combined with heart rate
  measures[\[26\]](https://esajournals.onlinelibrary.wiley.com/doi/10.1002/bes2.1801#:~:text=A%20Beginner%27s%20Guide%20to%20Conducting,original%20data%2C%20code%2C%20and%20documentation)."
  (Here we show both styles for illustration.) Every figure or table
  taken from another source should also have a citation in its caption.
- **Avoiding Plagiarism through Referencing:** Proper referencing
  enables readers to **locate the source of each quote or idea, and
  helps avoid plagiarism by acknowledging others'
  work[\[25\]](https://sruc.libguides.com/c.php?g=250237&p=4952318#:~:text=Referencing%20,UCL%20Institute%20of%20Education).**
  If you directly quote text (which should be done sparingly in
  technical reports), put it in quotation marks (or indented block for
  longer quotes) and cite the source with page number if applicable.
  More commonly, *paraphrase* the source's idea in your own words, and
  still give a citation. The rule is: **whenever an idea is not
  originally yours, give credit**. This extends to diagrams, data, and
  code -- if you adapted a figure or used a snippet of code from
  somewhere, note that in caption or code comment with a
  citation/reference.

By meeting these referencing standards, your documentation will show
academic rigor. It allows the project to be placed in context and
demonstrates that you have built on a foundation of existing knowledge
ethically and transparently. Good citation practice also showcases the
research you've done (which can strengthen the examiners' impression of
your work). Remember that referencing is not just a formality -- it is
about **integrity, giving credit, and enabling others to follow your
intellectual
trail**[\[24\]](https://studentsunionucl.org/academic-writing-guide#:~:text=Generally%20UCL%20uses%20either%20Harvard,your%20argument%20with%20other%20works).

## Academic Integrity and Plagiarism

Upholding **academic integrity** is paramount. UCL expects all work to
be **original or properly credited**. These principles must be reflected
in your writing and documentation practices:

- **No Plagiarism:** **Do not copy** others' work (text, data, or code)
  without proper attribution. Plagiarism is a serious academic offence.
  Ensure that any time you include or even closely paraphrase someone
  else's ideas, results, or phrasing, you provide an appropriate
  citation. If you find a sentence in a paper that perfectly fits your
  point, it is safer to paraphrase it in your own words and cite the
  source, rather than quote it directly -- excessive quoting can
  interrupt the flow and may raise questions. Under no circumstances
  include someone else's text or figures as if they were your own work.
  *When in doubt, cite the source.* This applies equally to less formal
  documentation (like READMEs) if they are part of the submission --
  they should not contain unattributed content from tutorials or
  websites either.
- **Understand and Apply UCL's Integrity Policies:** Familiarise
  yourself with UCL's **academic integrity guidelines** and **plagiarism
  policies**. These will outline what is expected in terms of
  originality and fair use of sources. **Being honest in your academic
  work and formally recognising the ideas on which your work is based is
  essential[\[27\]](https://www.ucl.ac.uk/students/academic-support/final-year-undergraduate/final-year-project#:~:text=Also%20remember%20to%20refresh%20and,could%20be%20penalised%20for%C2%A0Academic%20Misconduct).**
  If you fail to acknowledge someone else's work or ideas, you risk
  accusations of Academic
  Misconduct[\[27\]](https://www.ucl.ac.uk/students/academic-support/final-year-undergraduate/final-year-project#:~:text=Also%20remember%20to%20refresh%20and,could%20be%20penalised%20for%C2%A0Academic%20Misconduct).
  Use the available resources (such as the UCL library guides on
  avoiding plagiarism) to double-check that you are following best
  practices.
- **Cite Your Own Previous Work Appropriately:** If this project builds
  on a prior project or publication you authored (for example, a
  previous dissertation or a paper), do not recycle your own text
  without citation -- that is self-plagiarism. Instead, reference your
  earlier work as you would someone else's. It's fine to reuse some of
  your own code or design elements, but be transparent about it. In the
  thesis, you might say "This project extends the prototype developed in
  \[YourLastName,
  2024\][\[25\]](https://sruc.libguides.com/c.php?g=250237&p=4952318#:~:text=Referencing%20,UCL%20Institute%20of%20Education)"
  if applicable.
- **Acknowledge Contributions:** Clearly state if any part of the work
  was done in collaboration or with external help. For instance, if a
  colleague helped with collecting data or if you received significant
  advice on a design, acknowledge this in the appropriate section (e.g.,
  preface or acknowledgments section of the thesis). If you include any
  content (text, figures) provided by a supervisor or another student,
  mark it clearly and get permission and proper citation. UCL values
  transparency in who contributed to what.
- **Use of AI or Coding Assistance:** If you used tools like GitHub
  Copilot or other AI assistance for coding or writing, be cautious.
  Such tools may introduce unattributed external content. **Thoroughly
  review and edit any AI-generated suggestions** to ensure they meet
  these guidelines. It's best to use AI as a helper for ideas which you
  then write in your own words. Unless your department provides specific
  rules, you typically do not cite AI tools as sources for academic
  content; instead, treat them as you would an editing or brainstorming
  aid. However, the **intellectual content must ultimately be yours** --
  you are responsible for every piece of text in the documentation.
- **Plagiarism Checking:** Expect that your thesis will be run through
  plagiarism detection software (e.g., Turnitin). It's wise to run your
  own draft through such a tool if available to catch any inadvertent
  overlap with sources. If the similarity report flags any passages,
  rewrite them or ensure they have citations. Minor matches (common
  phrases or bibliography entries) are usually fine, but substantive
  matches with other texts are not.
- **Ethical Use of Source Code:** If you incorporate open-source code or
  hardware schematics into your project, make sure the licence allows it
  and **credit the source** (for example, in a code comment and in the
  dissertation text if it's a significant portion). Do not present
  borrowed code as solely your own creation. Even if the code is public
  domain, acknowledging the origin can be viewed positively as honesty
  and thoroughness.
- **Academic Honesty in Data:** Present data truthfully. Do not
  manipulate or cherry-pick results to mislead. If data points were
  excluded, state the reason (e.g., sensor failure). The integrity of
  how you report results is as important as avoiding text plagiarism.
  UCL's academic honesty extends to not falsifying or misrepresenting
  research findings.

By following these integrity practices, you demonstrate professionalism
and ethics. The examiners will trust your work more if it is clear that
you are not hiding anything and you credit all contributions fairly.
Remember, a strong project is built on **honesty and respect for the
work of others**, which ultimately reflects well on your own work.

## Ethical Considerations and Data Handling

For a project dealing with **biometric data and human-related
measurements**, UCL expects a thorough treatment of ethics and data
management in the documentation:

- **Ethics Approval:** If the project involved human participants (e.g.
  collecting GSR data from volunteers) or any form of human data, you
  **must obtain ethics approval** from the relevant UCL ethics committee
  *before* conducting the study. In your thesis and reports, **state
  whether ethical approval was required and obtained**. Include the
  reference number of the ethics approval and the approving body. For
  example: "Ethical approval for the experimental protocol was obtained
  from the UCL Research Ethics Committee (Project ID: 2025/1234)." If
  the project did not require human data or was a purely technical
  build, you should still mention that "No human participants were
  involved in this project, thus formal ethics approval was not
  required."
- **Informed Consent:** Document the process of obtaining **informed
  consent** if human subjects were involved. In the methodology section,
  note that participants were informed of the research purpose, what
  data was being collected (e.g., thermal video, GSR readings), and how
  it would be used. State that participants gave written (or documented)
  consent. If applicable, mention that they were free to withdraw at any
  time and that the study posed minimal risk. Providing this level of
  detail shows compliance with ethical research practices.
- **Data Anonymisation:** **Personal data protection** is crucial. If
  any personal identifiers were collected (even something like video of
  a person's face or their raw GSR data which could be considered
  personal health data), explain how you **anonymised or pseudonymized**
  the data. For instance, assign participant IDs instead of using names,
  blur faces in any saved video, and do not disclose any individually
  identifiable information in your thesis. In documentation, you can
  say, "Participant data were anonymised; each subject was assigned a
  code and no personally identifying information was stored with the GSR
  readings."
- **Data Handling and Storage:** Describe the measures taken for
  **secure data storage**. UCL expects compliance with data protection
  regulations (e.g., GDPR) for any personal or sensitive data. In
  practice, this means:
- Storing data on encrypted devices or drives if appropriate.
- Limiting access to the raw data (e.g., only the student and supervisor
  have access).
- Not transferring data insecurely. If cloud services were used, mention
  that they are approved services or note any data processing
  agreements.
- Disposing of data properly after the project (if required by ethics).
- You should include a brief statement like, "All collected data were
  stored securely on an encrypted UCL OneDrive folder, accessible only
  to the researcher and supervisor, in compliance with GDPR and UCL data
  management policies."
- **Compliance with Regulations:** Acknowledge relevant regulations or
  guidelines. For health-related projects, this might include stating
  compliance with the **Data Protection Act** and GDPR for data
  handling, and any relevant **health and safety standards** if the
  device could pose any risk. For example, if your device was used on
  people, confirm that it conforms to safety standards (like ISO
  standards for medical electrical equipment, if relevant) -- this can
  be both a technical point and an ethical one. If you're not actually
  measuring people in a medical sense, this may not apply deeply, but a
  sentence on ensuring the device is safe to use (no electrical risks,
  etc.) and that participants were comfortable is good practice.
- **Ethical Research Conduct in Documentation:** Reflect ethical
  considerations in how you write about the work. Be respectful and
  unbiased when describing participant-related information. Do not
  include any offensive or sensitive personal data in documentation. If
  you have a section discussing potential societal impacts or ethical
  implications of contactless GSR technology (which could be a
  worthwhile discussion in either Introduction or Conclusion), ensure
  it's thoughtfully written.
- **Environmental and Social Ethics (if applicable):** If the project
  has any environmental impact (e.g. electronic waste from devices,
  energy consumption) or social implications (privacy of monitoring
  stress levels), it might be worth noting how those were considered.
  UCL increasingly values sustainability and social responsibility, so
  mentioning, for instance, that the device was built with safe voltage
  levels and minimal power consumption, or that data will not be used
  beyond the project without consent, can show a holistic ethical
  awareness.
- **Integrating into Documentation:** Typically, the **Methodology**
  chapter includes a subsection on "Ethical Considerations". Make sure
  to include one. Also, the project proposal or interim report (if any)
  should have covered ethics -- you can draw from that. Ensure that
  whatever you promised in the ethics application (if there was one) is
  followed and described in the thesis. Consistency is key: the
  documentation must not contradict the ethics approval (e.g., if you
  said you'd recruit 20 people and only did 5, explain why; or if you
  said data will be confidential, ensure it truly is in the write-up).

Addressing ethics and data handling thoroughly in your documentation
shows **professionalism and adherence to UCL's research standards**. It
assures examiners that the project was conducted responsibly. It also
provides a model for future researchers to follow safe and ethical
practices, thereby strengthening the credibility of your work.

## Project Planning and Management Documentation

UCL MEng projects are not only evaluated on technical outcomes but also
on **project management and process**. Your documentation should
demonstrate that you planned and managed the project effectively:

- **Project Plan and Timeline:** Early in the thesis (often in the
  Introduction or a dedicated appendix), **include an overview of the
  project plan**. This might be a brief narrative and/or a Gantt chart
  showing the timeline of tasks. Indicate key milestones such as
  literature review phase, design phase, implementation, testing, and
  writing. A visual timeline can be very effective in an appendix, with
  a reference in text like "Appendix X presents the project Gantt chart,
  outlining the schedule of activities." Demonstrating explicit time
  planning highlights your organizational skills -- **time management is
  crucial to a successful final year
  project[\[28\]](https://www.ucl.ac.uk/students/academic-support/final-year-undergraduate/final-year-project#:~:text=Managing%20your%20time%20will%20be,proceeding%20to%20a%20Masters%20or%C2%A0Phd%C2%A0degree)**.
  It also allows you to reflect on whether the project proceeded as
  expected.
- **Milestones and Deliverables:** In the documentation, mention the
  main milestones or deliverables and when they were achieved. For
  example, "By December, a prototype of the sensor interface was
  completed and validated (Milestone 1)." This can be integrated into an
  Introduction or a Progress section if you have one. It shows that you
  set targets and met them (or adjusted as needed).
- **Changes to Plan:** It is common that projects evolve. If your
  project deviated from the initial plan (e.g., some features were
  descoped or new ones added), **document these changes**. You can write
  a short reflection, perhaps in the Conclusions or a dedicated section,
  on how the plan was adjusted and why. For instance: "Originally,
  integration with a cloud server was planned, but due to time
  constraints the focus shifted to on-device storage. This change is
  reflected in the updated plan (see Appendix Y) and ensured that core
  functionalities were delivered within the timeframe." Showing
  adaptability is positive, as long as you explain it.
- **Project Management Methodology:** Briefly describe how you managed
  the project. Did you use any **project management tools or
  techniques** (e.g., Agile sprints, weekly supervisor meetings, issue
  tracking on GitHub)? For example, "The project was managed using an
  iterative approach with bi-weekly sprints. Progress was tracked using
  a Trello board, and code development followed a Git workflow for
  version control." This can be a short paragraph, but it indicates to
  the examiner that you followed a systematic process.
- **Risk Management:** UCL expects students to be aware of risks in
  their projects and to have mitigation strategies. In documentation,
  you can include a table of key risks (technical, time, resource risks)
  and how you addressed them, either in an appendix or within a planning
  section. At minimum, in your text mention if any major risks
  materialised and what you did (e.g., "A risk was identified that the
  thermal camera might not have sufficient resolution; to mitigate this,
  a backup camera was prepared, though ultimately not needed."). This
  demonstrates foresight and comprehensive planning.
- **Regular Reporting:** Note if you had any interim reports or
  presentations (for instance, some departments require a mid-project
  report or design review). Indicate that those were completed and how
  feedback from them was integrated. It shows a feedback-responsive
  approach. For example: "A mid-year progress report was submitted in
  January, and feedback from the assessors (particularly on improving
  the calibration procedure) was incorporated into the project's next
  phase."
- **Reflection on Management:** In your conclusion or evaluation, it can
  be valuable to include a brief reflection on the project management
  aspect. For example: "The project was delivered on schedule, with all
  primary objectives met. Effective time management and early
  identification of technical challenges (see Section on Risk
  Management) contributed to this success. One lesson learned was the
  importance of buffer time for integrating components -- an initial
  delay in hardware delivery was absorbed by adjusting the schedule."
  This kind of reflective comment (just a few sentences) shows maturity
  and that you've learned from the process, which is often an aim of the
  MEng project.
- **Supporting Documents:** If your department requires it, include
  supporting project management documents. Some project handbooks want
  to see meeting logs, or a personal log of work. If you kept such a log
  (e.g., a diary of activities), you can mention it exists and offer it
  in appendices or separately. If not formally required, it's still good
  to show evidence of planning like the Gantt chart or milestone table
  as mentioned.

By integrating project planning and management details into your
documentation, you **highlight the process** behind the product. MEng
projects are not just graded on what you built or discovered, but also
on *how* you went about it. A well-documented plan and its execution
give insight into your workflow and can impress upon the examiners that
you worked methodically and professionally. It also helps future readers
understand the trajectory of the project, which can contextualise
decisions and outcomes.

## Clarity, Layout, and Presentation Standards

Clear language and a reader-friendly layout are essential to maximise
the impact of your documentation. UCL's guidelines often emphasise not
just what you write, but how you present it. Ensure the following
presentation standards are met in all documents:

- **Document Formatting:** Follow any formatting specifications given by
  UCL or your department (often outlined in the project handbook or UCL
  Academic Manual). Common requirements include:
- Standard A4 page size, with sufficient margins (usually \~2.5cm to
  3cm).
- A clear, professional font (e.g., Arial, Calibri, or Times New Roman)
  at an appropriate size (11 or 12 pt for body text).
- 1.5 line spacing or as required (to ensure readability and room for
  annotations).
- Page numbering (usually centred or right-bottom; make sure front
  matter is numbered in Roman numerals and main chapters in Arabic, if
  required by thesis format).
- Consistent alignment and justification (often left-aligned or
  justified text is expected -- check department preference; fully
  justified text looks neat but ensure spacing is not awkward).
- **Headings and Numbering:** Use a structured heading hierarchy and
  consider numbering sections and subsections for clarity (e.g., 1, 1.1,
  1.2, 2, 2.1, \...). Numbered headings make it easier to
  cross-reference sections in text. Ensure chapter titles and section
  headings are clearly distinguishable (e.g., by font size or bold
  formatting). The **inclusion of proper section headings and numbering
  aids coherent flow and navigation for the
  reader[\[29\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/module_deep_dive/test_academic_document.md#L10-L18)**.
- **Table of Contents and Lists of Figures/Tables:** The thesis should
  include a Table of Contents (with chapter and section titles and page
  numbers). If there are many figures and tables, include a List of
  Figures and List of Tables. This helps the reader quickly find
  information and demonstrates good organisation.
- **Figure and Table Presentation:** All figures and tables should be
  **neatly presented, labelled, and referenced**.
- Give each figure/table a **number and a descriptive caption**. For
  example, "Figure 4.2 -- Block diagram of the multi-sensor system
  architecture."
- Refer to every figure/table in the text (e.g., "as shown in Figure
  4.2, the system includes\...").
- Position figures/tables near the relevant text, but not before their
  first mention. Typically, figures are centred on the page. Tables
  should have a consistent style (e.g., visible borders or not, as long
  as they are clean and readable).
- If you adapted or took a figure from an external source, **cite the
  source in the caption** (e.g., "Source: \[Authors, Year\]") and
  include full details in references. If you created a figure using data
  from somewhere else, mention "Data source: \..." in the caption.
- Ensure text in figures (like labels in a diagram) is legible. Use
  vector graphics for diagrams if possible so they scale well. For
  plots, label axes with units and include legends.
- **Writing Style for Clarity:** On a micro level, ensure each sentence
  is clear. Avoid overly long sentences with many clauses; breaking them
  up can often improve clarity. Use punctuation effectively -- colons,
  semicolons, and commas can help break up ideas, but be careful not to
  overuse them. Read sentences out loud to see if they make sense in one
  go. Each paragraph should start with a **topic sentence** that
  introduces the main idea, making it easier to follow the argument.
- **Conciseness:** Aim for conciseness without sacrificing necessary
  detail. This means eliminate redundant phrases or repetitive
  information. However, **do not use unexplained acronyms or shorthand**
  for the sake of brevity -- always define terms on first use. Strike a
  balance between a **thorough explanation** and a **to-the-point
  delivery**. Remember, clarity is more important than trying to sound
  overly sophisticated.
- **Accessibility and Readability:** Keep in mind principles of
  accessible document design (as highlighted by UCL's ISD guides). Use
  high contrast between text and background (black text on white
  background is standard). Avoid very small fonts or dense blocks of
  text. Use lists (bulleted or numbered) to break up text where
  appropriate (as we are doing here) -- this helps readers scan key
  points quickly. The UCL guidance on creating accessible content
  emphasizes alignment, font, formatting, spacing, and layout to ensure
  readability for all
  audiences[\[30\]](https://www.ucl.ac.uk/students/academic-support/final-year-undergraduate/final-year-project#:~:text=The%20presentation%20of%20your%20final,to%20convert%20to%2Fcreate%20PDF%20documents).
  For example, a ragged-right margin (left aligned text) can sometimes
  be easier to read than fully justified, and using styles for headings
  ensures screen-readers can navigate the document structure.
- **Appendix and Referencing Layout:** In the appendices, format any
  source code listings or raw data clearly. Use monospaced font for
  code, with appropriate indentation. For large tables of data, consider
  rotating to landscape page orientation if it improves readability (and
  adjust page numbering accordingly). Make sure appendices are also
  listed in the table of contents. The referencing section should be
  formatted consistently as noted, and consider using hanging indent
  style for Harvard references or the IEEE specified format for numbered
  references.
- **Printing and Binding Considerations:** Although the submission might
  be digital, if you need to print and bind your thesis, adhere to UCL's
  binding requirements (often, soft bound for initial submission, hard
  bound after approval). Ensure that page margins are sufficient for
  binding. It's wise to check UCL's specifications (e.g., some require a
  certain colour cover or spine text for hardbound theses -- these
  specifics might apply more to doctoral theses, but be aware of any
  undergrad/MEng guidelines). Mentioning binding in documentation isn't
  needed, but from a planning perspective, leave time for
  printing/binding before the deadline, as noted in UCL
  guidelines[\[31\]](https://www.ucl.ac.uk/students/academic-support/final-year-undergraduate/final-year-project#:~:text=Binding%20your%20document%20is%20the,department%20before%20proceeding%20with%20binding).
- **Consistency across Documents:** All documents, whether the thesis
  PDF or a Markdown README on GitHub, should look polished. Consistently
  apply the same font and style where possible. For web-based docs (like
  a GitHub page), use clear markdown headings and lists so that the
  rendered document is easy to read. Consistency gives a professional
  impression that you paid attention to detail.

By focusing on clarity and presentation, you make it easier for
examiners and other readers to appreciate the content of your work. Poor
formatting or confusing writing can distract from the substance. On the
other hand, a well-structured and cleanly presented document conveys
**professionalism and allows the quality of the research to shine
through**. Always review your documents not just for technical accuracy
but also for readability -- possibly ask a peer to read for clarity, or
use UCL resources like the Writing Centre for feedback on the
presentation of your report.

## Reproducibility and Technical Accuracy

Finally, a core goal of academic documentation is to enable
**reproducibility** -- meaning another researcher or engineer could
replicate your work or validate your results given your documentation.
UCL and the broader research community value projects that are
documented with enough clarity and detail to be **reproducible and
technically accurate**. Ensure the following to support this:

- **Comprehensive Methodology Details:** Provide enough detail in your
  documentation for someone to **reproduce the experiments or system**.
  This includes specifying hardware models, versions of software,
  parameter values, and methodologies. For example, instead of saying
  "the sensors were calibrated regularly," detail **how** and **with
  what standard** you calibrated them. If you performed an experiment,
  describe the protocol step by step (you can use past tense narrative
  but include specifics: sample rate, duration, environment conditions,
  etc.). A reader should be able to follow your description and repeat
  the study in their own lab.
- **System Setup and Configuration:** In the technical documentation or
  appendices, list the exact components and setup configurations used.
  For instance:
- Hardware components with model numbers (e.g., *Topdon TC thermal
  camera, Shimmer3 GSR+ device, Samsung Galaxy S22 phone* for the mobile
  app).
- Software environment (e.g., *Android 15, Python 3.9, specific library
  versions*).
- Configuration files or settings (if your system has a config file,
  include a sample of it in an appendix or repository).
- Any calibration data or baseline data used. By providing this, you
  allow someone else to set up an identical environment. **Research is
  reproducible when others can reproduce the results given only the
  original data, code, and
  documentation[\[26\]](https://esajournals.onlinelibrary.wiley.com/doi/10.1002/bes2.1801#:~:text=A%20Beginner%27s%20Guide%20to%20Conducting,original%20data%2C%20code%2C%20and%20documentation)**
  -- strive for this ideal.
- **Availability of Code and Data:** Make sure the project's code is
  well-organised and **available** (to examiners, at least). If the code
  is hosted in a repository, provide a link in the documentation. If
  it's not public, include it in an appendix or as a submitted artifact.
  Similarly, key datasets or examples of recorded data should be
  provided if possible (within the bounds of ethical agreements -- e.g.,
  anonymised sample data). If data is too large, describe its nature and
  perhaps provide a way to obtain it. The principle is that nothing is
  hidden: *with your documentation, the code and data, one should be
  able to reconstruct the project results*.
- **Accurate Results Reporting:** Double-check all the numerical
  results, graphs, and tables in your documents against your raw data
  and analysis scripts. **Ensure that every figure and statistic in the
  report is accurate and consistent with the underlying data**.
  Technical accuracy also means that descriptions of the system reflect
  what was actually built -- for example, if you ended up not
  implementing a feature, do not claim that you did. Misrepresenting
  results or system capabilities, even unintentionally, can mislead
  readers and harm reproducibility. Before submission, verify the
  content: *if someone tries to follow my method, will they get the same
  result I claimed?* If an inconsistency is found (say, a bug was
  discovered after results were produced), it's better to explain it
  honestly in the thesis than to ignore it.
- **Validation and Testing Information:** To support reproducibility,
  include information on **how you validated the system and tested its
  performance**. For instance, provide details like "the synchronisation
  accuracy was tested by generating a known signal and measuring timing
  error between devices" with results. Also mention how many trials or
  samples your results are based on (e.g., "results are averaged over 5
  runs" or "10 participants' data were analysed"). This helps others
  understand the statistical significance and how to replicate tests. If
  applicable, include a note on random seed or randomness handling (for
  experiments with random elements, stating "a fixed random seed was
  used for repeatability" is useful).
- **Reproducibility of Analysis:** If you performed data analysis (e.g.,
  in Python or MATLAB), consider sharing the analysis scripts or
  describing the analysis steps in detail. For example, "GSR data was
  filtered with a 4th order Butterworth low-pass filter at 2 Hz, then
  peaks were detected using algorithm X (parameters\...); stress events
  were labelled as per Y criteria." These specifics allow another
  researcher to apply the same analysis to new data or verify your
  analysis on your data.
- **Technical Accuracy in Writing:** In your explanations, be precise.
  Avoid vague statements. Quantify claims wherever possible (e.g.,
  instead of "the system has high accuracy," say "the system achieved
  \<0.5 ms synchronisation error in 95% of samples"). If you reference
  equations or algorithms, ensure they are correct (derivations can be
  in an appendix if lengthy). A **technically accurate document** gains
  the trust of the reader -- it shows you are careful with details. It
  can help to have your supervisor or a peer review technical content
  for mistakes or ambiguities.
- **Peer Review and Proofreading:** Before finalizing, have someone else
  read the methodology and results sections specifically to see if they
  could replicate what you did. Sometimes what's obvious to the author
  is not to others. Their feedback can highlight missing pieces in the
  reproducibility puzzle. Additionally, run spelling and grammar checks;
  mistakes in technical terms or units can lead to confusion (e.g.,
  mixing up ms vs s).
- **Reproducibility Statement:** Some theses include a short statement
  about the reproducibility and data availability (this might not be
  mandatory, but it's a good touch). For instance, you could write in
  the Introduction or Conclusion: "All code and data required to
  reproduce the results in this thesis are available in the project
  repository or appendices. Detailed instructions have been provided to
  enable reproducibility of the experiments and system setup." This
  makes your intent clear and signals to examiners that you value open,
  verifiable research.

Adhering to these reproducibility and accuracy guidelines will greatly
enhance the **credibility of your project**. A reader will trust your
conclusions more if they can see exactly how you arrived at them and if
they could, in theory, reproduce the same findings step by step. Indeed,
**proper documentation not only enables future researchers to replicate
your work, but also ensures the quality and reliability of your current
data and results**. In summary, thorough and accurate documentation is
an investment in the long-term impact and integrity of your project.

------------------------------------------------------------------------

By following all the above guidelines, the documentation for the
*Multi-Sensor Recording System for Contactless GSR Prediction Research*
project will meet UCL's MEng academic requirements. The writing will be
formal and clear, the structure well-organised, the references properly
cited, and important aspects like ethics and reproducibility will be
well addressed. This not only fulfills academic obligations but also
produces a body of work that you and future readers can trust and build
upon. Good documentation is as important as good engineering in a
project of this nature -- it ensures that your hard work **can be
understood, evaluated, and carried forward** in the engineering and
research community. Good luck with your writing, and remember that
clarity and honesty in documentation are key to a successful MEng
project
report\![\[27\]](https://www.ucl.ac.uk/students/academic-support/final-year-undergraduate/final-year-project#:~:text=Also%20remember%20to%20refresh%20and,could%20be%20penalised%20for%C2%A0Academic%20Misconduct)[\[25\]](https://sruc.libguides.com/c.php?g=250237&p=4952318#:~:text=Referencing%20,UCL%20Institute%20of%20Education)

------------------------------------------------------------------------

[\[1\]](https://www.sjsu.edu/writingcenter/docs/handouts/First%20Person%20Usage%20in%20Academic%20Writing.pdf#:~:text=In%20most%20academic%20writing%2C%20first,is%20a%20lab%20report%2C%20the)
First Person Usage in Academic Writing

<https://www.sjsu.edu/writingcenter/docs/handouts/First%20Person%20Usage%20in%20Academic%20Writing.pdf>

[\[2\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L5-L13)
[\[3\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L15-L23)
[\[4\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L80-L88)
[\[5\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L90-L98)
[\[6\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L118-L126)
[\[7\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L134-L143)
[\[8\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L152-L160)
[\[9\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L222-L230)
[\[10\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L246-L254)
[\[11\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L230-L238)
[\[12\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L266-L274)
[\[13\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L298-L306)
[\[14\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L310-L318)
[\[15\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L354-L361)
[\[16\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md#L340-L348)
structure_full.md

<https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/thesis_report/structure_full.md>

[\[17\]](https://opensource.com/article/17/9/modular-documentation#:~:text=To%20make%20writing%20user%20story,fundamental%20content%20types%20provides%C2%A0several%20advantages)
[\[18\]](https://opensource.com/article/17/9/modular-documentation#:~:text=,overhead%20of%20formatting%20and%20structuring)
[\[19\]](https://opensource.com/article/17/9/modular-documentation#:~:text=overhead%20of%20formatting%20and%20structuring)
[\[20\]](https://opensource.com/article/17/9/modular-documentation#:~:text=,less%20daunting%20for%20new%20colleagues)
Modular documentation: How to make both writers and users happy \|
Opensource.com

<https://opensource.com/article/17/9/modular-documentation>

[\[21\]](https://subjectguides.york.ac.uk/referencing-style-guides/ieee#:~:text=IEEE%20,in%20electronics%20and%20related%20disciplines)
IEEE - Referencing styles - a Practical Guide - Subject Guides

<https://subjectguides.york.ac.uk/referencing-style-guides/ieee>

[\[22\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/module_deep_dive/test_academic_document.md#L4-L11)
[\[23\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/module_deep_dive/test_academic_document.md#L6-L8)
[\[29\]](https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/module_deep_dive/test_academic_document.md#L10-L18)
test_academic_document.md

<https://github.com/buccancs/bucika_gsr/blob/9d2d760e8228aa01eb67f66154debebb9cdf462a/docs/module_deep_dive/test_academic_document.md>

[\[24\]](https://studentsunionucl.org/academic-writing-guide#:~:text=Generally%20UCL%20uses%20either%20Harvard,your%20argument%20with%20other%20works)
Academic Writing Guide \| Students Union UCL

<https://studentsunionucl.org/academic-writing-guide>

[\[25\]](https://sruc.libguides.com/c.php?g=250237&p=4952318#:~:text=Referencing%20,UCL%20Institute%20of%20Education)
Referencing - Subject Guide: Engineering - LibGuides at Scotland\'s \...

<https://sruc.libguides.com/c.php?g=250237&p=4952318>

[\[26\]](https://esajournals.onlinelibrary.wiley.com/doi/10.1002/bes2.1801#:~:text=A%20Beginner%27s%20Guide%20to%20Conducting,original%20data%2C%20code%2C%20and%20documentation)
A Beginner\'s Guide to Conducting Reproducible Research

<https://esajournals.onlinelibrary.wiley.com/doi/10.1002/bes2.1801>

[\[27\]](https://www.ucl.ac.uk/students/academic-support/final-year-undergraduate/final-year-project#:~:text=Also%20remember%20to%20refresh%20and,could%20be%20penalised%20for%C2%A0Academic%20Misconduct)
[\[28\]](https://www.ucl.ac.uk/students/academic-support/final-year-undergraduate/final-year-project#:~:text=Managing%20your%20time%20will%20be,proceeding%20to%20a%20Masters%20or%C2%A0Phd%C2%A0degree)
[\[30\]](https://www.ucl.ac.uk/students/academic-support/final-year-undergraduate/final-year-project#:~:text=The%20presentation%20of%20your%20final,to%20convert%20to%2Fcreate%20PDF%20documents)
[\[31\]](https://www.ucl.ac.uk/students/academic-support/final-year-undergraduate/final-year-project#:~:text=Binding%20your%20document%20is%20the,department%20before%20proceeding%20with%20binding)
Final year project \| Students - UCL -- University College London

<https://www.ucl.ac.uk/students/academic-support/final-year-undergraduate/final-year-project>
