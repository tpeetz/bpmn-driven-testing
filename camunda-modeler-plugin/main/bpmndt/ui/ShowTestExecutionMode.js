import { MODE_SHOW_TEST_EXECUTION } from "../constants";

import BaseMode from "./BaseMode";

export default class ShowTestExecutionMode extends BaseMode {
  constructor(controller) {
    super(controller);

    this.id = MODE_SHOW_TEST_EXECUTION;
  }

  computeInitialState(ctx) {
    return {};
  }

  computeViewModel() {
  }
}
