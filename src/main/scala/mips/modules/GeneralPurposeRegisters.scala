package mips.modules

import chisel3._
import chisel3.util._

import mips.util.GPR._

class GeneralPurposeRegisters extends Module {
  val io = IO(new Bundle {
    val readId = Input(new RegisterReadId)
    val writeId = Input(UInt(5.W))
    val we = Input(Bool())
    val din = Input(UInt(32.W))
    val pcWrite = Input(SInt(32.W))

    val readData = Output(new RegisterReadData)
  })

  withClock((!clock.asBool).asClock) {
    val regs = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

    io.readData.data1 := regs(io.readId.id1)
    io.readData.data2 := regs(io.readId.id2)

    when (io.we && io.writeId =/= ZERO) {
      regs(io.writeId) := io.din
    }
  }

  when (io.pcWrite =/= 0.S && io.writeId =/= 0.U && io.we) {
    printf("@%x: $%d <= %x\n", io.pcWrite, io.writeId, io.din)
  }
}

object GeneralPurposeRegisters extends App {
  //  (new chisel3.stage.ChiselStage).emitVerilog(new GCD,args.union(Array("-td","v")).distinct)
  (new chisel3.stage.ChiselStage).emitVerilog(new GeneralPurposeRegisters, Array("-td", "v"))
}