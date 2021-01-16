package mips.util

import chisel3._
import chisel3.util._
import mips.modules.PC.JumpType

object Control {
  val reg_write_from_alu :: reg_write_from_mdu :: reg_write_from_dm :: reg_write_from_pc_8 :: reg_write_from_imme_lsh :: Nil = Enum(5)
  val alu_op_from_reg1 :: alu_op_from_reg2 :: alu_op_from_uimme :: alu_op_from_simme :: alu_op_from_sha :: Nil = Enum(5)
  val dm_write_from_reg2 = 0.U(1.W)
  val jump_val_from_reg1 :: jump_val_from_uimme :: jump_val_from_simme :: Nil = Enum(3)
  val reg_id_zero :: reg_id_rs :: reg_id_rt :: reg_id_rd :: reg_id_ra :: Nil = Enum(5)
  val jump_never :: jump_always :: jump_eq :: jump_neq :: jump_ltz :: jump_lez :: jump_gtz :: jump_gez :: Nil = Enum(8)
  val reg_stage_decode :: reg_stage_exec :: reg_stage_mem :: reg_stage_none :: Nil = Enum(4)

  def getRegisterId(reg: UInt, inst: Instruction) : UInt = {
    val a = Wire(UInt(5.W))
    a := MuxCase(GPR.ZERO, Array(
      (reg === reg_id_zero) -> GPR.ZERO,
      (reg === reg_id_rs) -> inst.sourseReg,
      (reg === reg_id_rt) -> inst.targetReg,
      (reg === reg_id_rd) -> inst.destReg,
      (reg === reg_id_ra) -> GPR.RA
    ))
    a
  }

  def getJumpValue(jump: UInt, reg1: UInt, inst: Instruction) : UInt = {
    val a = Wire(UInt(32.W))
    a := MuxCase(GPR.ZERO, Array(
      (jump === jump_val_from_reg1) -> reg1,
      (jump === jump_val_from_simme) -> inst.imm16.asTypeOf(SInt(32.W)).asUInt(),
      (jump === jump_val_from_uimme) -> inst.imm16.asTypeOf(UInt(32.W)),
    ))
    a
  }

  def getOperand(from: UInt, regs: Vec[UInt], inst: Instruction) : UInt = {
    val a = Wire(UInt(32.W))
    a := MuxCase(0x00CC00CC.U(32.W), Array(
      (from === alu_op_from_reg1) -> regs(0),
      (from === alu_op_from_reg2) -> regs(1),
      (from === alu_op_from_simme) -> inst.imm16.asTypeOf(SInt(32.W)).asUInt(),
      (from === alu_op_from_uimme) -> inst.imm16.asTypeOf(UInt(32.W)),
      (from === alu_op_from_sha) -> inst.shiftAmount.asTypeOf(UInt(32.W))
    ))
    a
  }
}

class ControlSignal extends Bundle {
  val reg1IDFrom = chiselTypeOf(Control.reg_id_zero)
  val reg2IDFrom = chiselTypeOf(Control.reg_id_zero)
  val regData1Stage = chiselTypeOf(Control.reg_stage_none)
  val regData2Stage = chiselTypeOf(Control.reg_stage_none)
  val wRegIDFrom = chiselTypeOf(Control.reg_id_zero)
  val wRegEnabled = Bool()
  val wRegDataFrom = chiselTypeOf(Control.reg_write_from_alu)
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
