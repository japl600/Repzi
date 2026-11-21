package com.japl.repzi.core.testing

import com.japl.repzi.core.domain.AppVisibility

/** The app is on screen until a test says otherwise. */
class FakeAppVisibility(var visible: Boolean = true) : AppVisibility {
    override fun isVisible(): Boolean = visible
}
