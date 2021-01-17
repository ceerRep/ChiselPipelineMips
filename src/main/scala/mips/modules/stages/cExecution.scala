package mips.modules.stages

import chisel3._
import chisel3.util._
import mips.modules.{ALU, MDU}
import mips.util.BypassRegData._
import mips.util.pipeline._
import mips.util.Control._
import mips.util._

class cExecution extends Module {
  val io = IO(new Bundle{
    val pipelineDecodeResult = Input(new DecodeBundle)

    val pipelineExecutionResult = Output(new ExecutionBundle)
    val suspendOnExecution = Output(new Bool())
    val suspendFromExecution = Output(new Bool())
    val dataFromExec = Output(new BypassRegData)

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
  })
  val dataFromExec = Wire(new BypassRegData)
  io.dataFromExec := dataFromExec

  val pipelineExecutionResult = RegInit(0.U.asTypeOf(new ExecutionBundle))
  io.pipelineExecutionResult := pipelineExecutionResult

  val regDataNotReady = Wire(Vec(2, Bool()))

  io.bypass.pcMagic := io.pipelineDecodeResult.pcMagic
  io.bypass.regReadId := io.pipelineDecodeResult.regReadId
  io.bypass.origRegData := io.pipelineDecodeResult.regReadData

  val regReadData1 = io.bypass.bypassData(0).data
  val regReadData2 = io.bypass.bypassData(1).data

  regDataNotReady(1) := io.bypass.bypassData(0).suspend
  regDataNotReady(0) := io.bypass.bypassData(1).suspend

  val regData = Wire(GPR.registerReadData)
  regData(0) := regReadData1
  regData(1) := regReadData2

  val mduBusy = Wire(Bool())
  val suspendFromExecution =
    (regDataNotReady(0) && io.pipelineDecodeResult.controlSignal.regData1Stage <= reg_stage_exec) ||
    (regDataNotReady(1) && io.pipelineDecodeResult.controlSignal.regData1Stage <= reg_stage_exec) ||
    (mduBusy && io.pipelineDecodeResult.controlSignal.useMdu)
  io.suspendFromExecution := suspendFromExecution

  val suspendOnExecution = suspendFromExecution
  io.suspendOnExecution := suspendOnExecution

  val suspend = suspendOnExecution || io.pipelineDecodeResult.bubbled

  val op1 = getOperand(io.pipelineDecodeResult.controlSignal.aluOpFrom1, regData, io.pipelineDecodeResult.inst)
  val op2 = getOperand(io.pipelineDecodeResult.controlSignal.aluOpFrom2, regData, io.pipelineDecodeResult.inst)

  val alu = Module(new ALU)
  alu.io.op1 := op1
  alu.io.op2 := op2
  alu.io.op := io.pipelineDecodeResult.controlSignal.aluOp

  val mdu = Module(new MDU)
  mdu.io.op1 := op1
  mdu.io.op2 := op2
  mdu.io.op := io.pipelineDecodeResult.controlSignal.mduOp
  mdu.io.start := io.pipelineDecodeResult.controlSignal.startMdu && !suspend
  mduBusy := mdu.io.busy

  val passBubble = suspendFromExecution || io.pipelineDecodeResult.bubbled

  when (!suspend) {
    pipelineExecutionResult.pc := io.pipelineDecodeResult.pc
    pipelineExecutionResult.pcMagic := io.pipelineDecodeResult.pcMagic
    pipelineExecutionResult.inst := io.pipelineDecodeResult.inst
    pipelineExecutionResult.controlSignal := io.pipelineDecodeResult.controlSignal
    pipelineExecutionResult.regReadId := io.pipelineDecodeResult.regReadId
    pipelineExecutionResult.regReadData := regData
    pipelineExecutionResult.aluResult := alu.io.result
    pipelineExecutionResult.dmAddr := alu.io.result
    pipelineExecutionResult.wRegId := io.pipelineDecodeResult.wRegId

    when (io.pipelineDecodeResult.controlSignal.wRegEnabled) {
      when (io.pipelineDecodeResult.wRegDataReady) {
        pipelineExecutionResult.wRegDataReady := 1.B
        pipelineExecutionResult.wRegData := io.pipelineDecodeResult.wRegData
      }.elsewhen(io.pipelineDecodeResult.controlSignal.wRegDataFrom === reg_write_from_alu) {
        pipelineExecutionResult.wRegDataReady := 1.B
        pipelineExecutionResult.wRegData := alu.io.result
      }.elsewhen(io.pipelineDecodeResult.controlSignal.wRegDataFrom === reg_write_from_mdu) {
        pipelineExecutionResult.wRegDataReady := 1.B
        pipelineExecutionResult.wRegData := mdu.io.data
      }.otherwise {
        pipelineExecutionResult.wRegDataReady := 0.B
        pipelineExecutionResult.wRegData := 0x5AAA555A.U(32.W)
      }
    }.otherwise {
      pipelineExecutionResult.wRegDataReady := 1.B
      pipelineExecutionResult.wRegData := 0x0D0D000E.U(32.W)
    }
  }

  pipelineExecutionResult.bubbled := passBubble

  when (pipelineExecutionResult.bubbled) {
    dataFromExec.regId := GPR.ZERO
    dataFromExec.dataReady := 1.B
    dataFromExec.data := 0.U
  }.otherwise {
    dataFromExec.regId := pipelineExecutionResult.wRegId
    dataFromExec.dataReady := pipelineExecutionResult.wRegDataReady
    dataFromExec.data := pipelineExecutionResult.wRegData
  }

}

object cExecution extends App {
  //  (new chisel3.stage.ChiselStage).emitVerilog(new GCD,args.union(Array("-td","v")).distinct)
  (new chisel3.stage.ChiselStage).emitVerilog(new cExecution, Array("-td", "v"))
}
