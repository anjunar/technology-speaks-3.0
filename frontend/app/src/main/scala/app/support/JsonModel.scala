package app.support

import jfx.form.Model

trait JsonModel[M] extends Model[M] { self: M => }
