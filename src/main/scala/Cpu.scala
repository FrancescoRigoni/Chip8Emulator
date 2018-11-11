
class Cpu(val memory: Memory) {

  private val regVX = Array.ofDim[Byte](16)
  private var regI:Short = _
  private var regVF:Boolean = _

  private var regSound:Byte = _
  private var regDelay:Byte = _

  private var regPC:Int = Memory.PROGRAM_START
  private var regSP:Byte = _

  private var stack = List[Short]()

  private var random = 123
  private val randomIncrement = 78

  def decrementDelay : Unit = {
    if (regDelay != 0) regDelay = (regDelay - 1).toByte
  }

  private def pop : Short = {
    val value = stack.head
    stack = stack.tail
    value
  }

  private def push(value:Short) = {
    stack = value :: stack
  }

  def fetch : Short = {
    val value = memory.ramReadTwoBytes(regPC.toShort)
    regPC = regPC + 2
    value
  }

  private def getXandKK(instruction:Short) : (Byte, Byte) = {
    val x: Byte = ((instruction & 0x0F00) >> 8).toByte
    val kk: Byte = (instruction & 0x00FF).toByte
    (x, kk)
  }

  private def getX(instruction:Short) : Byte = {
    ((instruction & 0x0F00) >> 8).toByte
  }

  private def getXandY(instruction:Short) : (Byte, Byte) = {
    val x: Byte = ((instruction & 0x0F00) >> 8).toByte
    val y: Byte = ((instruction & 0x00F0) >> 4).toByte
    (x, y)
  }

  private def getXYN(instruction:Short) : (Byte, Byte, Byte) = {
    val x: Byte = ((instruction & 0x0F00) >> 8).toByte
    val y: Byte = ((instruction & 0x00F0) >> 4).toByte
    val n: Byte = (instruction & 0x000F).toByte
    (x, y, n)
  }

  private def getNNN(instruction:Short) : Short = {
    (instruction & 0x0FFF).toShort
  }

  private def getRandom() : Byte = {
    random = random + randomIncrement
    random.toByte
  }

  def execute(instr : Short) : Unit = {
    val realPC = regPC - Memory.PROGRAM_START
    instr match {
      case v if v == 0x00EE =>
        // Return from subroutine
        regPC = pop

      case v if v == 0x00E0 =>
        // Clear display
        memory.clearVideo()

      case v if (v & 0xF0FF) == 0xE09E =>
        println("Not implemented")
      // Skip next instruction if key with the value of Vx is pressed
      // TODO

      case v if (v & 0xF0FF) == 0xE0A1 =>
        println("Not implemented")
      // Skip next instruction if key with the value of Vx is not pressed
      // TODO

      case v if (v & 0xF0FF) == 0xF007 =>
        // Set Vx = delay timer value
        val x = getX(v)
        regVX(x) = regDelay

      case v if (v & 0xF0FF) == 0xF00A =>
      // Wait for a key press, store the value of the key in Vx
      // TODO

      case v if (v & 0xF0FF) == 0xF015 =>
        // Set delay timer = Vx
        val x = getX(v)
        regDelay = regVX(x)

      case v if (v & 0xF0FF) == 0xF018 =>
        // Set sound timer = Vx
        val x = getX(v)
        regSound = regVX(x)

      case v if (v & 0xF0FF) == 0xF01E =>
        // Set I = I + Vx
        val x = getX(v)
        regI = (regI + regVX(x).toShort).toShort

      case v if (v & 0xF0FF) == 0xF029 =>
        // Set I = location of sprite for digit Vx
        val x = getX(v)
        regI = (regVX(x) * 5).toShort

      case v if (v & 0xF0FF) == 0xF033 =>
        // Store BCD representation of Vx in memory locations I, I+1, and I+2
        val x = getX(v)
        val hundreds = regVX(x) / 100
        val tenths = (regVX(x) % 100) / 10
        val units = (regVX(x) % 100) % 10
        memory.ramStartStoring(regI)
        memory.ramStoreByte(hundreds.toByte)
        memory.ramStoreByte(tenths.toByte)
        memory.ramStoreByte(units.toByte)
        memory.ramFinishStoring()

      case v if (v & 0xF0FF) == 0xF055 =>
        // Store registers V0 through Vx in memory starting at location I
        val x = getX(v)
        memory.ramStartStoring(regI)
        (0 to x).foreach { v => memory.ramStoreByte(regVX(v)) }
        memory.ramFinishStoring()

      case v if (v & 0xF0FF) == 0xF065 =>
        // Read registers V0 through Vx from memory starting at location I
        val x = getX(v)
        (0 to x).foreach { v => regVX(v) = memory.ramReadByte((regI + v).toShort) }

      case v if (v & 0xF000) == 0x1000 =>
        // Jump to location nnn
        regPC = getNNN(v)

      case v if (v & 0xF000) == 0x2000 =>
        // Call subroutine at nnn.
        push(regPC.toShort)
        regPC = getNNN(v)

      case v if (v & 0xF000) == 0x3000 =>
        // Skip next instruction if Vx = kk
        val (x, kk) = getXandKK(v)
        if (regVX(x) == kk) regPC = regPC + 2

      case v if (v & 0xF000) == 0x4000 =>
        // Skip next instruction if Vx != kk.
        val (x, kk) = getXandKK(v)
        if (regVX(x) != kk) regPC = regPC + 2

      case v if (v & 0xF000) == 0x5000 =>
        // Skip next instruction if Vx = Vy.
        val (x, y) = getXandY(v)
        if (regVX(x) == regVX(y)) regPC += 2

      case v if (v & 0xF000) == 0x6000 =>
        // Set Vx = kk.
        val (x, kk) = getXandKK(v)
        regVX(x) = kk

      case v if (v & 0xF000) == 0x7000 =>
        // Set Vx = Vx + kk
        val (x, kk) = getXandKK(v)
        regVX(x) = (regVX(x) + kk).toByte

      case v if (v & 0xF00F) == 0x8000 =>
        // Set Vx = Vy
        val (x, y) = getXandY(v)
        regVX(x) = regVX(y)

      case v if (v & 0xF00F) == 0x8001 =>
        // Set Vx = Vx OR Vy
        val (x, y) = getXandY(v)
        regVX(x) = (regVX(x) | regVX(y)).toByte

      case v if (v & 0xF00F) == 0x8002 =>
        // Set Vx = Vx AND Vy
        val (x, y) = getXandY(v)
        regVX(x) = (regVX(x) & regVX(y)).toByte

      case v if (v & 0xF00F) == 0x8003 =>
        // Set Vx = Vx XOR Vy
        val (x, y) = getXandY(v)
        regVX(x) = (regVX(x) ^ regVX(y)).toByte

      case v if (v & 0xF00F) == 0x8004 =>
        // Vx = Vx + Vy, set VF = carry
        val (x, y) = getXandY(v)
        val addition = regVX(x) + regVX(y)
        regVX(x) = addition.toByte
        regVF = addition > 255

      case v if (v & 0xF00F) == 0x8005 =>
        // Set Vx = Vx - Vy, set VF = NOT borrow
        val (x, y) = getXandY(v)
        regVF = regVX(x) > regVX(y)
        regVX(x) = (regVX(x) - regVX(y)).toByte

      case v if (v & 0xF00F) == 0x8006 =>
        // Set Vx = Vx SHR 1
        val (x, _) = getXandY(v)
        regVF = (regVX(x) & 0x1) == 1
        regVX(x) = (regVX(x) / 2).toByte

      case v if (v & 0xF00F) == 0x8007 =>
        // Set Vx = Vy - Vx, set VF = NOT borrow
        val (x, y) = getXandY(v)
        regVF = regVX(y) > regVX(x)
        regVX(x) = (regVX(y) - regVX(x)).toByte

      case v if (v & 0xF00F) == 0x8008 =>
        // Set Vx = Vx SHL 1
        val (x, _) = getXandY(v)
        regVF = (regVX(x) & 0x80) == 0x80
        regVX(x) = (regVX(x) * 2).toByte

      case v if (v & 0xF00F) == 0x8009 =>
        // Skip next instruction if Vx != Vy
        val (x, y) = getXandY(v)
        if (regVX(x) != regVX(y)) regPC += 2

      case v if (v & 0xF000) == 0xA000 =>
        // I = nnn
        regI = getNNN(v)

      case v if (v & 0xF000) == 0xB000 =>
        // Jump to location nnn + V0
        regPC = getNNN(v) + regVX(0)

      case v if (v & 0xF000) == 0xC000 =>
        // Set Vx = random byte AND kk
        val (x, kk) = getXandKK(v)
        regVX(x) = (getRandom() & kk.toByte).toByte

      case v if (v & 0xF000) == 0xD000 =>
        // Display n-byte sprite starting at memory location I at (Vx, Vy), set VF = collision.
        val (x, y, n) = getXYN(v)
        regVF = memory.showSprite(n, regI, regVX(x), regVX(y))

      case _ => {
        println("Not implemented! " + instr)
        System.exit(1)
      }
    }
  }
}
