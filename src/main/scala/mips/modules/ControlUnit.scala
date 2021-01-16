package mips.modules

import chisel3._
import PCControl.JumpType
import mips.util.Control._
import mips.util.Ops._
import mips.util.InstructionByteCode._
import mips.util.{ControlSignal, Instruction}

class ControlUnit extends Module {
  val io = IO(new Bundle {
    val inst = Input(new Instruction)
    val controlSignal = Output(new ControlSignal)
  })

  io.controlSignal.reg1IDFrom := reg_id_zero
  io.controlSignal.reg2IDFrom := reg_id_zero
  io.controlSignal.regData1Stage := reg_stage_none
  io.controlSignal.regData2Stage := reg_stage_none
  io.controlSignal.wRegIDFrom := reg_id_zero
  io.controlSignal.wRegEnabled := 0.B
  io.controlSignal.wRegDataFrom := reg_write_from_alu
  io.controlSignal.dmReadSize := 0.B
  io.controlSignal.dmReadSigned := 0.B
  io.controlSignal.aluOpFrom1 := alu_op_from_reg1
  io.controlSignal.aluOpFrom2 := alu_op_from_reg1
  io.controlSignal.aluOp := add
  io.controlSignal.mduOp := m_read_hi
  io.controlSignal.useMdu := 0.B
  io.controlSignal.startMdu := 0.B
  io.controlSignal.dmWriteSize := 0.U
  io.controlSignal.dmWriteDataFrom := dm_write_from_reg2
  io.controlSignal.jumpCond := jump_never
  io.controlSignal.jumpType := JumpType.far
  io.controlSignal.jumpValFrom := jump_val_from_reg1

  val inst32 = WireDefault(io.inst.inst)

  when(inst32 === ADD || inst32 === ADDU || inst32 === SUB || inst32 === SUBU ||
    inst32 === SLL || inst32 === SRL || inst32 === SRA || inst32 === SLLV ||
    inst32 === SRLV || inst32 === SRAV || inst32 === AND || inst32 === OR ||
    inst32 === XOR || inst32 === NOR || inst32 === SLT || inst32 === SLTU) {

    io.controlSignal.reg2IDFrom := reg_id_rt
    io.controlSignal.regData1Stage := reg_stage_exec
    io.controlSignal.regData2Stage := reg_stage_exec
    io.controlSignal.aluOpFrom2 := alu_op_from_reg2
    io.controlSignal.wRegEnabled := 1.B
    io.controlSignal.wRegIDFrom := reg_id_rd
    io.controlSignal.wRegDataFrom := reg_write_from_alu

    when(inst32 === SLL || inst32 === SRL || inst32 === SRA) {
      io.controlSignal.reg1IDFrom := reg_id_zero
      io.controlSignal.aluOpFrom1 := alu_op_from_sha
    }.otherwise {
      io.controlSignal.reg1IDFrom := reg_id_rs
      io.controlSignal.aluOpFrom1 := alu_op_from_reg1
    }

    when(inst32 === ADD || inst32 === ADDU) {
      io.controlSignal.aluOp := add
    }.elsewhen(inst32 === SUB || inst32 === SUBU) {
      io.controlSignal.aluOp := sub
    }.elsewhen(inst32 === SLL || inst32 === SLLV) {
      io.controlSignal.aluOp := shift_l
    }.elsewhen(inst32 === SRL || inst32 === SRLV) {
      io.controlSignal.aluOp := shift_r_l
    }.elsewhen(inst32 === SRA || inst32 === SRAV) {
      io.controlSignal.aluOp := shift_r_a
    }.elsewhen(inst32 === AND) {
      io.controlSignal.aluOp := and
    }.elsewhen(inst32 === OR) {
      io.controlSignal.aluOp := or
    }.elsewhen(inst32 === XOR) {
      io.controlSignal.aluOp := xor
    }.elsewhen(inst32 === NOR) {
      io.controlSignal.aluOp := nor
    }.elsewhen(inst32 === SLT) {
      io.controlSignal.aluOp := sless
    }.elsewhen(inst32 === SLTU) {
      io.controlSignal.aluOp := uless
    }
  }.elsewhen(inst32 === ADDI || inst32 === ADDIU || inst32 === ANDI || inst32 === ORI ||
    inst32 === XORI || inst32 === SLTI || inst32 === SLTIU) {

    io.controlSignal.reg1IDFrom := reg_id_rs
    io.controlSignal.regData1Stage := reg_stage_exec
    io.controlSignal.regData2Stage := reg_stage_exec
    io.controlSignal.aluOpFrom1 := alu_op_from_reg1
    io.controlSignal.wRegDataFrom := reg_write_from_alu
    io.controlSignal.wRegIDFrom := reg_id_rt
    io.controlSignal.wRegEnabled := 1.B

    when(inst32 === ADDI) {
      io.controlSignal.aluOpFrom2 := alu_op_from_simme
      io.controlSignal.aluOp := add
    }.elsewhen(inst32 === ADDIU) {
      io.controlSignal.aluOpFrom2 := alu_op_from_simme
      io.controlSignal.aluOp := add
    }.elsewhen(inst32 === ANDI) {
      io.controlSignal.aluOpFrom2 := alu_op_from_uimme
      io.controlSignal.aluOp := and
    }.elsewhen(inst32 === ORI) {
      io.controlSignal.aluOpFrom2 := alu_op_from_uimme
      io.controlSignal.aluOp := or
    }.elsewhen(inst32 === XORI) {
      io.controlSignal.aluOpFrom2 := alu_op_from_uimme
      io.controlSignal.aluOp := xor
    }.elsewhen(inst32 === SLTI) {
      io.controlSignal.aluOpFrom2 := alu_op_from_simme
      io.controlSignal.aluOp := sless
    }.elsewhen(inst32 === SLTIU) {
      io.controlSignal.aluOpFrom2 := alu_op_from_simme
      io.controlSignal.aluOp := uless
    }
  }.elsewhen(inst32 === BEQ || inst32 === BNE || inst32 === BLEZ || inst32 === BGTZ ||
    inst32 === BGEZ || inst32 === BLTZ) {

    io.controlSignal.reg1IDFrom := reg_id_rs
    io.controlSignal.regData1Stage := reg_stage_decode
    io.controlSignal.regData2Stage := reg_stage_decode
    io.controlSignal.jumpType := JumpType.relative
    io.controlSignal.jumpValFrom := jump_val_from_simme

    when(inst32 === BEQ) {
      io.controlSignal.reg2IDFrom := reg_id_rt
      io.controlSignal.jumpCond := jump_eq
    }.elsewhen(inst32 === BNE) {
      io.controlSignal.reg2IDFrom := reg_id_rt
      io.controlSignal.jumpCond := jump_neq
    }.elsewhen(inst32 === BLEZ) {
      io.controlSignal.reg2IDFrom := reg_id_zero
      io.controlSignal.jumpCond := jump_lez
    }.elsewhen(inst32 === BGTZ) {
      io.controlSignal.reg2IDFrom := reg_id_zero
      io.controlSignal.jumpCond := jump_gtz
    }.elsewhen(inst32 === BGEZ) {
      io.controlSignal.reg2IDFrom := reg_id_zero
      io.controlSignal.jumpCond := jump_gez
    }.elsewhen(inst32 === BLTZ) {
      io.controlSignal.reg2IDFrom := reg_id_zero
      io.controlSignal.jumpCond := jump_ltz
    }
  }.elsewhen(inst32 === LUI) {
    io.controlSignal.wRegEnabled := 1.B
    io.controlSignal.wRegIDFrom := reg_id_rt
    io.controlSignal.wRegDataFrom := reg_write_from_imme_lsh
  }.elsewhen(inst32 === J || inst32 === JAL) {
    when(inst32 === JAL) {
      io.controlSignal.wRegEnabled := 1.B
      io.controlSignal.wRegIDFrom := reg_id_ra
      io.controlSignal.wRegDataFrom := reg_write_from_pc_8
    }
    io.controlSignal.jumpCond := jump_always
    io.controlSignal.jumpType := JumpType.near
    io.controlSignal.jumpValFrom := jump_val_from_uimme
  }.elsewhen(inst32 === JR || inst32 === JALR) {
    when (inst32 === JALR) {
      io.controlSignal.wRegEnabled := 1.B
      io.controlSignal.wRegIDFrom := reg_id_rd
      io.controlSignal.wRegDataFrom := reg_write_from_pc_8
    }
    io.controlSignal.reg1IDFrom := reg_id_rs
    io.controlSignal.regData1Stage := reg_stage_decode
    io.controlSignal.jumpCond := jump_always
    io.controlSignal.jumpType := JumpType.far
    io.controlSignal.jumpValFrom := jump_val_from_reg1
  }.elsewhen(inst32 === LB || inst32 === LBU || inst32 === LH || inst32 === LHU ||
    inst32 === LW || inst32 === SB || inst32 === SH || inst32 === SW) {

    io.controlSignal.reg1IDFrom := reg_id_rs
    io.controlSignal.aluOpFrom1 := alu_op_from_reg1
    io.controlSignal.aluOpFrom2 := alu_op_from_simme
    io.controlSignal.aluOp := add

    when(inst32 === LB || inst32 === LBU || inst32 === LH || inst32 === LHU || inst32 === LW) {
      io.controlSignal.regData1Stage := reg_stage_exec
      io.controlSignal.wRegEnabled := 1.B
      io.controlSignal.wRegIDFrom := reg_id_rt
      io.controlSignal.wRegDataFrom := reg_write_from_dm

      when (inst32 === LB || inst32 === LH) {
        io.controlSignal.dmReadSigned := 1.B
      }.elsewhen(inst32 === LBU || inst32 === LHU) {
        io.controlSignal.dmReadSigned := 0.B
      }

      when (inst32 === LW) {
        io.controlSignal.dmReadSize := 3.U
      }.elsewhen(inst32 === LH || inst32 === LHU) {
        io.controlSignal.dmReadSize := 2.U
      }.elsewhen(inst32 === LB || inst32 === LBU) {
        io.controlSignal.dmReadSize := 1.U
      }
    }.elsewhen(inst32 === SB || inst32 === SH || inst32 === SW) {
      io.controlSignal.regData1Stage := reg_stage_exec
      io.controlSignal.regData2Stage := reg_stage_mem
      io.controlSignal.reg2IDFrom := reg_id_rt
      io.controlSignal.dmWriteDataFrom := dm_write_from_reg2

      when (inst32 === SB) {
        io.controlSignal.dmWriteSize := 1.U
      }.elsewhen(inst32 === SH) {
        io.controlSignal.dmWriteSize := 2.U
      }.elsewhen(inst32 === SW) {
        io.controlSignal.dmWriteSize := 3.U
      }
    }
  }.elsewhen(inst32 === MULT || inst32 === MULTU || inst32 === DIV || inst32 === DIVU) {
    io.controlSignal.reg1IDFrom := reg_id_rs
    io.controlSignal.reg2IDFrom := reg_id_rt
    io.controlSignal.regData1Stage := reg_stage_exec
    io.controlSignal.regData2Stage := reg_stage_exec
    io.controlSignal.aluOpFrom1 := alu_op_from_reg1
    io.controlSignal.aluOpFrom2 := alu_op_from_reg2
    io.controlSignal.useMdu := 1.B
    io.controlSignal.startMdu := 1.B

    when (inst32 === MULT) {
      io.controlSignal.mduOp := m_sign_mul
    }.elsewhen(inst32 === MULTU) {
      io.controlSignal.mduOp := m_unsign_mul
    }.elsewhen(inst32 === DIV) {
      io.controlSignal.mduOp := m_sign_div
    }.elsewhen(inst32 === DIVU) {
      io.controlSignal.mduOp := m_unsign_div
    }
  }.elsewhen(inst32 === MFHI || inst32 === MFLO) {
    io.controlSignal.useMdu := 1.B
    io.controlSignal.wRegIDFrom := reg_id_rd
    io.controlSignal.wRegEnabled := 1.B
    io.controlSignal.wRegDataFrom := reg_write_from_mdu

    when (inst32 === MFHI) {
      io.controlSignal.mduOp := m_read_hi
    }.elsewhen(inst32 === MFLO) {
      io.controlSignal.mduOp := m_read_lo
    }
  }.elsewhen(inst32 === MTHI || inst32 === MTLO) {
    io.controlSignal.useMdu := 1.B
    io.controlSignal.reg1IDFrom := reg_id_rs
    io.controlSignal.regData1Stage := reg_stage_exec
    io.controlSignal.aluOpFrom1 := alu_op_from_reg1

    when (inst32 === MTHI) {
      io.controlSignal.mduOp := m_write_hi
    }.elsewhen(inst32 === MTLO) {
      io.controlSignal.mduOp := m_write_lo
    }
  }
}

object ControlUnit extends App {
  //  (new chisel3.stage.ChiselStage).emitVerilog(new GCD,args.union(Array("-td","v")).distinct)
  (new chisel3.stage.ChiselStage).emitVerilog(new ControlUnit, Array("-td", "v"))
}