package mips.util

import chisel3._
import mips.modules.PCControl.JumpType

class ControlSignal extends Bundle {
  val reg1IDFrom = chiselTypeOf(Control.reg_id_zero)
  val reg2IDFrom = chiselTypeOf(Control.reg_id_zero)
  val wRegIDFrom = chiselTypeOf(Control.reg_id_zero)
  val wRegDataFrom = chiselTypeOf(Control.reg_write_from_alu)
  val regData1Stage = chiselTypeOf(Control.reg_stage_none)
  val regData2Stage = chiselTypeOf(Control.reg_stage_none)
  val wRegEnabled = Bool()
  val dmReadSize = UInt(2.W)
  val dmReadSigned = Bool()
  val aluOpFrom1 = chiselTypeOf(Control.alu_op_from_reg1)
  val aluOpFrom2 = chiselTypeOf(Control.alu_op_from_reg1)
  val aluOp = chiselTypeOf(Ops.add)
  val mduOp = chiselTypeOf(Ops.m_read_hi)
  val useMdu = Bool()
  val startMdu = Bool()
  val dmWriteSize = UInt(2.W)
  val dmWriteDataFrom = chiselTypeOf(Control.dm_write_from_reg2)
  val jumpCond = chiselTypeOf(Control.jump_always)
  val jumpType = chiselTypeOf(JumpType.far)
  val jumpValFrom = chiselTypeOf(Control.jump_val_from_reg1)
}