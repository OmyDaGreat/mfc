import kotlin.system.exitProcess

/**
 * The xyz.malefic.mfc.main function to run the CommitGenerator.
 * @param args The command-line arguments.
 */
fun main(args: Array<String>) {
    if (args.isNotEmpty() && args[0] == "hello") {
        println("Hello World!")
    } else {
        exitProcess(1)
    }
}
