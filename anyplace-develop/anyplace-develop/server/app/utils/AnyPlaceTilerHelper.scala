package utils

import play.Logger
import java.io._
import java.nio.file.Files
import org.apache.commons.io._

//remove if not needed
import scala.collection.JavaConversions._

object AnyPlaceTilerHelper {

    private val ANYPLACE_TILER_SCRIPTS_DIR = "anyplace_tiler"

    private val ANYPLACE_TILER_SCRIPT_START = ANYPLACE_TILER_SCRIPTS_DIR + File.separatorChar + "start-anyplace-tiler.sh"
  //private val ANYPLACE_TILER_SCRIPT_START = ANYPLACE_TILER_SCRIPTS_DIR + File.separatorChar + "tiler.bat"

    private val FLOOR_PLANS_ROOT_DIR = "floor_plans" + File.separatorChar

    private val FLOOR_TILES_DIR = "static_tiles" + File.separatorChar

    val FLOOR_TILES_ZIP_NAME = "tiles_archive.zip"

    def getRootFloorPlansDir(): String = FLOOR_PLANS_ROOT_DIR

    def getRootFloorPlansDirFor(buid: String): String = {
        getRootFloorPlansDir + buid + File.separatorChar
    }

    def getRootFloorPlansDirFor(buid: String, floor: String): String = {
        if (buid.trim().isEmpty || floor.trim().isEmpty) {
            return null
        }
        getRootFloorPlansDirFor(buid) + "fl_" + floor + File.separatorChar
    }

    def getFloorPlanFor(buid: String, floor: String): String = {
        if (buid.trim().isEmpty || floor.trim().isEmpty) {
            return null
        }
        getRootFloorPlansDirFor(buid, floor) + "fl_" + floor
    }

    def getFloorTilesDirFor(buid: String, floor: String): String = {
        if (buid.trim().isEmpty || floor.trim().isEmpty) {
            return null
        }
        getRootFloorPlansDirFor(buid, floor) + FLOOR_TILES_DIR
    }

    def getFloorTilesZipFor(buid: String, floor: String): String = {
        if (buid.trim().isEmpty || floor.trim().isEmpty) {
            return null
        }
        getRootFloorPlansDirFor(buid, floor) + FLOOR_TILES_DIR +
          FLOOR_TILES_ZIP_NAME
    }

    def getFloorTilesZipLinkFor(buid: String, floor: String): String = {
        if (buid.trim().isEmpty || floor.trim().isEmpty) {
            return null
        }
        AnyplaceServerAPI.SERVER_FULL_URL + File.separatorChar +
          "anyplace/floortiles/" +
          buid +
          File.separatorChar +
          floor +
          File.separatorChar +
          FLOOR_TILES_ZIP_NAME
    }

    def storeFloorPlanToServer(buid: String, floor_number: String, file: File): File = {
        val dirS = AnyPlaceTilerHelper.getRootFloorPlansDirFor(buid, floor_number)
        val dir = new File(dirS)
        dir.mkdirs()
        if (!dir.isDirectory || !dir.canWrite() || !dir.canExecute()) {
            throw new AnyPlaceException("Floor plans directory is inaccessible!!!")
        }
        val name = "fl" + "_" + floor_number
        val dest_f = new File(dir, name)
        var fout: FileOutputStream = null
        fout = new FileOutputStream(dest_f)
        Files.copy(file.toPath(), fout)
        fout.close()
        dest_f
    }

    def tileImage(imageFile: File, lat: String, lng: String): Boolean = {
        if (!imageFile.isFile || !imageFile.canRead()) {
            return false
        }
        val imageDir = imageFile.getParentFile
        if (!imageDir.isDirectory || !imageDir.canWrite() || !imageDir.canRead()) {
            throw new AnyPlaceException("Server do not have the permissions to tile the passed argument[" +
              imageFile.toString +
              "]")
        }

        val debug_ab = imageFile.getAbsolutePath.toString
        val debug_ab_temp1 = FilenameUtils.getPath(imageFile.getAbsolutePath.toString)
        val debug_ab_temp2 = FilenameUtils.separatorsToUnix(debug_ab_temp1)
        val debug_ab_cygwin = "/cygdrive/c/" + debug_ab_temp2  + FilenameUtils.getName(imageFile.getAbsolutePath.toString)

       // val pb = new ProcessBuilder("cmd","/c","C:\\cygwin64\\bin\\mintty.exe", "/cygdrive/c/Work/Project/HSM/Anyplace_Develop/anyplace-develop/anyplace-develop/server/anyplace_tiler/start-anyplace-tiler.sh", debug_ab_cygwin, lat,
       //    lng, "-DISLOG")

       // val pb = new ProcessBuilder("C:\\cygwin64\\bin\\bash.exe", "-c", "--login","-i", "/cygdrive/c/Work/Project/HSM/Anyplace_Develop/anyplace-develop/anyplace-develop/server/anyplace_tiler/start-anyplace-tiler.sh", debug_ab_cygwin, lat,
        //   lng, "-ENLOG")

      val pb = new ProcessBuilder("C:\\Work\\Project\\HSM\\Anyplace_Develop\\anyplace-develop\\anyplace-develop\\server\\anyplace_tiler\\Cygwin_Anyplace_Test.bat", debug_ab_cygwin , lat, lng, "-ENLOG" )

 //       val pb = new ProcessBuilder("\"C:\\Program Files (x86)\\Git\\bin\\bash.exe\" -c \"C:\\Work\\Project\\HSM\\Anyplace_Develop\\anyplace-develop\\anyplace-develop\\server\"" + ANYPLACE_TILER_SCRIPT_START, imageFile.getAbsolutePath.toString, lat,
 //                 lng, "-DISLOG")
        // val pb = new ProcessBuilder("C:\\cygwin64\\bin\\bash.exe", "/C","C:\\Work\\Project\\HSM\\Anyplace_Develop\\anyplace-develop\\anyplace-develop\\server\\anyplace_tiler\\start-anyplace-tiler.sh" , imageFile.getAbsolutePath.toString, lat,
         //   lng, "-ENLOG")

        //C:\Windows\System32 Irina
        val log = new File(imageDir, "anyplace_tiler_" + imageFile.getName + ".log")
        pb.redirectErrorStream(true)
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log))
        try {
            val p = pb.start()
            val is = p.getInputStream
            val br = new BufferedReader(new InputStreamReader(is))
            var line = br.readLine()
            while (line != null) {
                println(">" + line)
                line = br.readLine()
            }
            p.waitFor()
            if (p.exitValue() != 0) {
                val err = "Tiling for image[" + imageFile.toString + "] failed with exit code[" +
                  p.exitValue() +
                  "]!"
                Logger.error(err)
                throw new AnyPlaceException(err)
            }
        } catch {
            case e: IOException => {
                val err = "Tiling for image[" + imageFile.toString + "] failed with IOException[" +
                  e.getMessage +
                  "]!"
                Logger.error(err)
                throw new AnyPlaceException(err)
            }
            case e: InterruptedException => {
                val err = "Tiling for image[" + imageFile.toString + "] failed with InterruptedException[" +
                  e.getMessage +
                  "]!"
                Logger.error(err)
                throw new AnyPlaceException(err)
            }
        }
        true
    }
}
