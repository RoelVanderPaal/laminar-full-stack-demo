import {splitModuleId} from "./helpers";
import MagicString from "magic-string";

/**
 * This plugin lets you import JS files or resources like CSS for their
 * side effects only, that is, ignore the imported value:
 *
 * `import "stylesheet.css"`
 *
 * To get this code, call JsImportSideEffect("stylesheet.css") in your code.
 * This requires the JsImportSideEffect.scala interface file, and the JSGlobal
 * name in that file must match the `importFnName` option passed to this plugin.
 *
 * This is needed for Vite to process CSS imports from Scala.js files properly.
 *
 * Scala.js itself does something like `import * as unused from "stylesheet.css"`,
 * which is technically equivalent to `import "stylesheet.css"`, but Vite treats
 * it differently, throwing a "Default and named imports from CSS files are deprecated"
 * warning.
 *
 * This plugin works in the following manner:
 * - JsImportSideEffect("stylesheet.css") emits `foo("stylesheet.css")` JS code
 * - This plugin replaces all instances of `foo(` in your JS files with `import (`,
 *   so that you have `import ("stylesheet.css")` in your code, which is equivalent
 *   to import "stylesheet.css".
 * - Therefore, the `foo` name must be a unique string in your codebase,
 *   and must match the `importFnName` option that you provide to this plugin.
 *
 * See:
 * - https://discord.com/channels/632150470000902164/635668814956068864/1161984220814643241
 * - https://vitejs.dev/guide/features.html#disabling-css-injection-into-the-page
 * - https://github.com/vitejs/vite/pull/10762
 * - https://github.com/vitejs/vite/issues/3246
 * - https://github.com/scala-js/scala-js/issues/4156
 */
export default function importSideEffectPlugin (options) {
  if (!options || !options.importFnName) {
    throw new Error("Regex replacer vite plugin: please provide options object with `importFnName` key")
  }
  if (!options.sourceMapOptions) {
    // See https://github.com/rich-harris/magic-string#sgeneratemap-options-
    options.sourceMapOptions = {};
  }
  if (options.sourceMapOptions.hires === undefined) {
    options.sourceMapOptions.hires = true; // high resolution source maps by default
  }
  return {
    name: 'import-side-effect',
    transform (code, id) {
      const {moduleId, querySuffix} = splitModuleId(id);

      // #TODO We only want to process JS files generated by scala.js,
      //  processing any other files is a waste of CPU cycles.
      if (moduleId.endsWith(".js") && !moduleId.includes("node_modules")) {
        // console.log(`>> import-side-effect processing module ${id}...`)
        const str = new MagicString(code);
        const pattern = new RegExp(options.importFnName + "\\(", 'g');
        const replacement = 'import (';

        let hasReplacements = false;
        let match = null;
        while ((match = pattern.exec(code)) !== null) {
          // console.log(">>    replacing!")
          hasReplacements = true;
          str.overwrite(match.index, match.index + match[0].length, replacement);
        }

        if (hasReplacements) {
          return {
            code: str.toString(),
            map: str.generateMap(options.sourceMapOptions)
          };
        } else {
          return null;
        }
      }
    }
  };
}
