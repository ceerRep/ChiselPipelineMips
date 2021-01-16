package mips.util

import chisel3._
import InstructionConstants._

object InstParse {
  def parseInstruction(inst32: UInt) : Instruction = {
    val inst = Wire(new Instruction)
    inst.inst := inst32
    inst.sourseReg := inst32(INST_RS_HIGH, INST_RS_LOW)
    inst.targetReg := inst32(INST_RT_HIGH, INST_RT_LOW)
    inst.destReg := inst32(INST_RD_HIGH, INST_RD_LOW)
    inst.shiftAmount := inst32(INST_SH_HIGH, INST_SH_LOW)
    inst.imm16 := inst32(INST_IMME_HIGH, INST_IMME_LOW)
    inst.absulute_jump := inst32(INST_ADDR_HIGH, INST_ADDR_LOW)
    inst
  }
}
