

object Main extends App {
  val programName = "demo.ch8"

  println("+++ CHIP8 Emulator started +++")
  val memory = new Memory
  println("Testing memory")
  if (!MemoryTest.test(memory)) {
    println("Memory test failed")
    System.exit(1)
  }

  println("Loading interpreter")
  val interpreter = new Interpreter(memory)
  interpreter.loadSelf()

  println("Loading " + programName)
  val programSize = interpreter
    .loadProgram(programName)
    .getOrElse {
      System.exit(1)
      0
  }

  println("Loaded " + programName + " : " + programSize + " bytes" )

  val millisecondsFor60Hz = 1000 / 60

  val cpu = new Cpu(memory)
  while(true) {
    val instruction = cpu.fetch
    cpu.execute(instruction)
    //memory.dumpVideo()

    Thread.sleep(millisecondsFor60Hz)
    cpu.decrementDelay
  }

}