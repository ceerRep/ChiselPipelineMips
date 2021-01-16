package mips.modules.stages

import chisel3._
import chisel3.util._
import mips.DataMemoryBlackBox
import mips.util.BypassRegData._
import mips.util.pipeline._
import mips.util.Control._
import mips.util._

class dMemory extends Module {
  val io = IO(new Bundle {
    val pipelineExecutionResult = Input(new ExecutionBundle)

    val pipelineMemoryResult = Output(new MemoryBundle)
    val dataFromMem = Output(new BypassRegData)

    // Bypass Query
    val bypass = new Bundle {
      val pcMagic = Output(Bool())
      val regReadId = Output(GPR.registerReadId)
      val origRegData = Output(GPR.registerReadData)
      val bypassData = Input(Vec(2, new Bundle {
        val data = UInt(32.W)
        val suspend = Bool()
      }))
    }

    val mem = new Bundle {
      val pc = Output(SInt(32.W))
      val addr = Output(UInt(32.W))
      val readSize = Output(UInt(2.W))
      val readSign = Output(Bool())
      val writeSize = Output(UInt(2.W))
      val writeData = Output(UInt(32.W))

      val readData = Input(UInt(32.W))
    }
  })
  val dataFromMem = Wire(new BypassRegData)
  io.dataFromMem := dataFromMem

  val pipelineMemoryResult = RegInit(0.U.asTypeOf(new MemoryBundle))
  io.pipelineMemoryResult := pipelineMemoryResult

  val regDataNotReady = Wire(Vec(2, Bool()))

  io.bypass.pcMagic := io.pipelineExecutionResult.pcMagic
  io.bypass.regReadId := io.pipelineExecutionResult.regReadId
  io.bypass.origRegData := io.pipelineExecutionResult.regReadData

  val regReadData1 = io.bypass.bypassData(0).data
  val regReadData2 = io.bypass.bypassData(1).data

  regDataNotReady(1) := io.bypass.bypassData(0).suspend
  regDataNotReady(0) := io.bypass.bypassData(1).suspend

  val regData = Wire(GPR.registerReadData)
  regData(0) := regReadData1
  regData(1) := regReadData2

  assert((regDataNotReady(0) || regDataNotReady(1)) === 0.B)

  val suspend = io.pipelineExecutionResult.bubbled

  io.mem.addr := io.pipelineExecutionResult.dmAddr
  io.mem.readSize := io.pipelineExecutionResult.controlSignal.dmReadSize
  io.mem.readSign := io.pipelineExecutionResult.controlSignal.dmReadSigned
  io.mem.writeSize := Mux(suspend, 0.U, io.pipelineExecutionResult.controlSignal.dmWriteSize)
  io.mem.writeData := Mux(io.pipelineExecutionResult.controlSignal.dmWriteDataFrom === dm_write_from_reg2,
    regData(1),
    0.U
  )
  io.mem.pc := io.pipelineExecutionResult.pc

  val passBubble = io.pipelineExecutionResult.bubbled

  when (!suspend) {
    pipelineMemoryResult.pc := io.pipelineExecutionResult.pc
    pipelineMemoryResult.pcMagic := io.pipelineExecutionResult.pcMagic
    pipelineMemoryResult.inst := io.pipelineExecutionResult.inst
    pipelineMemoryResult.controlSignal := io.pipelineExecutionResult.controlSignal
    pipelineMemoryResult.regReadId := io.pipelineExecutionResult.regReadId
    pipelineMemoryResult.regReadData := io.pipelineExecutionResult.regReadData
    pipelineMemoryResult.dmAddr := io.pipelineExecutionResult.dmAddr
    pipelineMemoryResult.wRegId := io.pipelineExecutionResult.wRegId

    when (io.pipelineExecutionResult.controlSignal.wRegEnabled) {
      when (io.pipelineExecutionResult.wRegDataReady) {
        pipelineMemoryResult.wRegDataReady := 1.B
        pipelineMemoryResult.wRegData := io.pipelineExecutionResult.wRegData
      }.elsewhen(io.pipelineExecutionResult.controlSignal.wRegDataFrom === reg_write_from_dm) {
        pipelineMemoryResult.wRegDataReady := 1.B
        pipelineMemoryResult.wRegData := io.mem.readData
      }.otherwise{
        pipelineMemoryResult.wRegDataReady := 0.B
        pipelineMemoryResult.wRegData := 0x123455AA.U
      }
    }.otherwise {
      pipelineMemoryResult.wRegDataReady := 1.B
      pipelineMemoryResult.wRegData := 0x0DE0BEEF.U
    }
  }

  pipelineMemoryResult.bubbled := passBubble

  when (pipelineMemoryResult.bubbled) {
    dataFromMem.regId := GPR.ZERO
    dataFromMem.dataReady := 1.B
    dataFromMem.data := 0.U
  }.otherwise {
    dataFromMem.regId := pipelineMemoryResult.wRegId
    dataFromMem.dataReady := pipelineMemoryResult.wRegDataReady
    dataFromMem.data := pipelineMemoryResult.wRegData
  }

}

object dMemory extends App {
  //  (new chisel3.stage.ChiselStage).emitVerilog(new GCD,args.union(Array("-td","v")).distinct)
  (new chisel3.stage.ChiselStage).emitVerilog(new dMemory, Array("-td", "v"))
}