package mips.util

object InstructionConstants {
  val INST_OP_HIGH        = 31
  val INST_OP_LOW         = 26

  val INST_RS_HIGH        = 25
  val INST_RS_LOW         = 21
  val INST_RT_HIGH        = 20
  val INST_RT_LOW         = 16
  val INST_RD_HIGH        = 15
  val INST_RD_LOW         = 11

  // R only
  val INST_SH_HIGH      = 10
  val INST_SH_LOW       = 6
  val INST_FUNC_HIGH    = 5
  val INST_FUNC_LOW     = 0

  // I only
  val INST_IMME_HIGH    = 15
  val INST_IMME_LOW     = 0

  // J only
  val INST_ADDR_HIGH    = 25
  val INST_ADDR_LOW     = 0
}
