import {
  MODE_EDIT,
  MODE_MIGRATE,
  MODE_SELECT,
  MODE_SHOW_COVERAGE,
  MODE_SHOW_TEST_EXECUTION,
  MODE_VIEW,
  UNSUPPORTED_ELEMENT_TYPES
} from "./constants";

import { getMarkers } from "./functions";

import PathFinder from "./PathFinder";
import PathMarker from "./PathMarker";
import PathValidator from "./PathValidator";
import TestCase from "./TestCase";
import TestCaseMigration from "./TestCaseMigration";
import TestCaseModdle from "./TestCaseModdle";

import EditMode from "./ui/EditMode";
import MigrateMode from "./ui/MigrateMode";
import SelectMode from "./ui/SelectMode";
import ShowCoverageMode from "./ui/ShowCoverageMode";
import ShowTestExecutionMode from "./ui/ShowTestExecutionMode";
import ViewMode from "./ui/ViewMode";

export default class PluginController {
  constructor(options) {
    this.bpmnModelChanged = false;
    this.elementRegistry = options.elementRegistry;
    this.enabled = false;
    this.hidePlugin = options.hidePlugin;
    this.overlays = options.overlays;
    this.pathFinder = new PathFinder(options);
    this.pathMarker = new PathMarker(options);
    this.pathValidator = new PathValidator(options);
    this.testCases = [];
    this.testCaseModdle = new TestCaseModdle(options);

    this._addedOverlays = [];

    // modes
    this._editMode = new EditMode(this);
    this._migrateMode = new MigrateMode(this);
    this._selectMode = new SelectMode(this);
    this._showCoverageMode = new ShowCoverageMode(this);
    this._showTestExecutionMode = new ShowTestExecutionMode(this);
    this._viewMode = new ViewMode(this);
  }

  addOverlays(overlays) {
    this._removeOverlays();

    for (const overlay of overlays) {
      const { flowNodeId, html, position, type } = overlay;

      let element;
      if (flowNodeId === undefined) {
        element = this.testCaseModdle.process;
      } else {
        element = this.elementRegistry.get(flowNodeId);
      }

      overlay.id = this.overlays.add(element, type, { html, position });
      this._addedOverlays.push(overlay);
    }
  }

  disable() {
    this.enabled = false;
    this.pathMarker.unmark();
    this._removeOverlays();
  }

  enable() {
    const { mode, testCases } = this;

    if (mode !== undefined) {
      // recompute initial state of last active mode
      this.setMode(mode.id, mode.initialCtx);
    } else if (testCases.length !== 0) {
      this.setMode(MODE_VIEW, this);
    } else {
      this.setMode(MODE_SELECT, this);
    }

    this._validateTestCases();
    this.enabled = true;
  }

  findPaths(start, end) {
    return this.pathFinder.find(start, end);
  }

  handleBpmnElementChanged(oldProperties, properties) {
    if (!oldProperties.id) {
      return;
    }

    // when element ID was changed, update the path of each test case
    this.testCases.forEach(testCase => testCase.updateFlowNodeId(oldProperties.id, properties.id));
  }

  handleBpmnElementClicked(event) {
    const { element } = event;

    if (UNSUPPORTED_ELEMENT_TYPES.has(element.type)) {
      // skip click on unsupported element
      return;
    }

    const { mode } = this;

    if (mode.id === MODE_SELECT) {
      mode.handleSelection(element.id);
    }
  }

  handleBpmnModelChanged() {
    this.bpmnModelChanged = true;
  }

  handleLoadTestCases() {
    const { testCaseModdle } = this;

    if (!testCaseModdle.findProcess()) {
      // if process element could not be found
      return;
    }

    this.testCases = testCaseModdle.getTestCases();

    // allow initial validation
    this.bpmnModelChanged = true;

    this._validateTestCases();
  }

  handleSaveTestCases() {
    const { testCases, testCaseModdle } = this;
    testCaseModdle.setTestCases(testCases);
  }

  handleToggleMode(modeId) {
    const { mode, testCases } = this;

    if (mode.id === MODE_SELECT && modeId === MODE_MIGRATE) {
      // special case
      this.setMode(MODE_VIEW, this);
    } else if (mode.id !== modeId) {
      // toggle mode
      this.setMode(modeId, this);
    } else if (modeId === MODE_SELECT && mode.isMigration()) {
      // special case
      this.setMode(MODE_MIGRATE, {testCase: mode.state.migration.testCase, testCases: testCases});
    } else {
      // disable current mode and enable view mode
      this.setMode(MODE_VIEW, this);
    }
  }

  mark(markers) {
    this.pathMarker.mark(markers);
  }

  markAsChanged() {
    this.testCaseModdle.markAsChanged();
  }

  markError() {
    this.pathMarker.markError();
  }

  setMode(modeId, ctx) {
    this._removeOverlays();

    this.mode = this._getModeById(modeId);

    // remember initial context
    // so the mode can be recomputed when plugin is enabled/disabled
    this.mode.initialCtx = ctx;

    // triggers update
    this.mode.setState(this.mode.computeInitialState(ctx));
  }

  update() {
    this.state = this._computeState();
    this.updateView();
  }

  updateTestExecutionMode() {
    const { enabled, mode } = this;

    if (!enabled) {
      return;
    }

    if (mode && mode.id === MODE_SHOW_TEST_EXECUTION) {
      // recompute initial state of mode
      this.setMode(mode.id, mode.initialCtx);
    }
  }

  _autoResolveProblem(testCase) {
    const problem = testCase.problems.find(problem => problem.autoResolvable);
    if (problem === undefined) {
      return;
    }

    const migration = new TestCaseMigration(testCase, problem);

    migration.migrate(new TestCase({path: problem.paths[0]}));

    const { enabled, mode, pathMarker } = this;

    this.markAsChanged();

    if (enabled && mode.id === MODE_VIEW && mode.testCase === testCase) {
      pathMarker.mark(getMarkers(testCase));
    }
  }

  _computeState() {
    const { mode } = this;
    const { id, state } = mode;

    const isMigration = id === MODE_SELECT && mode.isMigration();

    const activeModes = {};
    activeModes[MODE_EDIT] = id === MODE_EDIT;
    activeModes[MODE_SELECT] = id === MODE_SELECT;
    activeModes[MODE_SHOW_COVERAGE] = id === MODE_SHOW_COVERAGE;
    activeModes[MODE_SHOW_TEST_EXECUTION] = id === MODE_SHOW_TEST_EXECUTION;

    // special case
    activeModes[MODE_MIGRATE] = id === MODE_MIGRATE || isMigration;

    // hide view to make diagram completely clickable
    let hideView = id === MODE_SHOW_COVERAGE;
    hideView = hideView || (id === MODE_SELECT && state.paths.length === 0);
    hideView = hideView || (id === MODE_VIEW && state.testCases.length === 0);

    return {
      activeModes: activeModes,
      showView: !hideView,
      viewModel: mode.computeViewModel()
    };
  }

  _getModeById(modeId) {
    switch (modeId) {
      case MODE_EDIT:
        return this._editMode;
      case MODE_MIGRATE:
        return this._migrateMode;
      case MODE_SELECT:
        return this._selectMode
      case MODE_SHOW_COVERAGE:
        return this._showCoverageMode;
      case MODE_SHOW_TEST_EXECUTION:
        return this._showTestExecutionMode;
      case MODE_VIEW:
        return this._viewMode;
      default:
        throw new Error(`Unsupported mode '${modeId}'`);
    }
  }

  _removeOverlays() {
    for (const overlay of this._addedOverlays) {
      this.overlays.remove(overlay.id);
    }

    this._addedOverlays = [];
  }

  _validateTestCases() {
    const { bpmnModelChanged, pathValidator, testCases } = this;

    if (!bpmnModelChanged || testCases.length === 0) {
      // nothing to validate
      return;
    }

    this.bpmnModelChanged = false;

    setTimeout(() => {
      testCases.forEach((testCase) => {
        testCase.problems = pathValidator.validate(testCase);

        this._autoResolveProblem(testCase);
      });

      if (this.enabled) {
        this.update();
      }
    }, 1000);
  }
}
