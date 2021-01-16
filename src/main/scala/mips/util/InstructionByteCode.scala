package mips.util

import chisel3.util.BitPat

object InstructionByteCode {
  val ADD     = BitPat("b000000????????????????????100000")
  val ADDU    = BitPat("b000000????????????????????100001")
  val SUB     = BitPat("b000000????????????????????100010")
  val SUBU    = BitPat("b000000????????????????????100011")
  val SLL     = BitPat("b000000????????????????????000000")
  val SRL     = BitPat("b000000????????????????????000010")
  val SRA     = BitPat("b000000????????????????????000011")
  val SLLV    = BitPat("b000000????????????????????000100")
  val SRLV    = BitPat("b000000????????????????????000110")
  val SRAV    = BitPat("b000000????????????????????000111")
  val AND     = BitPat("b000000????????????????????100100")
  val OR      = BitPat("b000000????????????????????100101")
  val XOR     = BitPat("b000000????????????????????100110")
  val NOR     = BitPat("b000000????????????????????100111")
  val SLT     = BitPat("b000000????????????????????101010")
  val SLTU    = BitPat("b000000????????????????????101011")

  val ADDI    = BitPat("b001000??????????????????????????")
  val ADDIU   = BitPat("b001001??????????????????????????")
  val ANDI    = BitPat("b001100??????????????????????????")
  val ORI     = BitPat("b001101??????????????????????????")
  val XORI    = BitPat("b001110??????????????????????????")
  val SLTI    = BitPat("b001010??????????????????????????")
  val SLTIU   = BitPat("b001011??????????????????????????")

  val LUI     = BitPat("b001111??????????????????????????")

  val MULT    = BitPat("b000000????????????????????011000")
  val MULTU   = BitPat("b000000????????????????????011001")
  val DIV     = BitPat("b000000????????????????????011010")
  val DIVU    = BitPat("b000000????????????????????011011")
  val MFHI    = BitPat("b000000????????????????????010000")
  val MTHI    = BitPat("b000000????????????????????010001")
  val MFLO    = BitPat("b000000????????????????????010010")
  val MTLO    = BitPat("b000000????????????????????010011")

  val BEQ     = BitPat("b000100??????????????????????????")
  val BNE     = BitPat("b000101??????????????????????????")
  val BLEZ    = BitPat("b000110??????????????????????????")
  val BGTZ    = BitPat("b000111??????????????????????????")
  val BGEZ    = BitPat("b000001?????00001????????????????")
  val BLTZ    = BitPat("b000001?????00000????????????????")

  val JR      = BitPat("b000000????????????????????001000")
  val JALR    = BitPat("b000000????????????????????001001")
  val J       = BitPat("b000010??????????????????????????")
  val JAL     = BitPat("b000011??????????????????????????")

  val LB      = BitPat("b100000??????????????????????????")
  val LBU     = BitPat("b100100??????????????????????????")
  val LH      = BitPat("b100001??????????????????????????")
  val LHU     = BitPat("b100101??????????????????????????")
  val LW      = BitPat("b100011??????????????????????????")
  val SB      = BitPat("b101000??????????????????????????")
  val SH      = BitPat("b101001??????????????????????????")
  val SW      = BitPat("b101011??????????????????????????")

  val SYSCALL = BitPat("b000000????????????????????001100")
};
