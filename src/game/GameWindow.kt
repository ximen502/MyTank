package game

import game.map.*
import java.awt.Color
import java.awt.Graphics
import java.awt.Image
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.JFrame

class GameWindow(width: Int, height: Int, windowTitle: String) : JFrame(), GOObserver {
    private var w: Int = 0
    private var h: Int = 0
    private var t: String = ""

    private var fps: Int = 0

    private var tempGraphics: Graphics? = null
    private var tempImage: Image? = null

    private var renderThread: RenderThread? = null

    private var darkAI: DarkAI? = null

    private var ground: Ground//? = null

    private var player: Tank

    //    var list = mutableListOf<GameObject>()
    //为解决ConcurrentModificationException，使用了如下的线程安全的容器类
    var list = CopyOnWriteArrayList<GameObject>()

    // 瓦片地图容器
    var tileList = mutableListOf<GameObject>()

    private var input: Input
    private val SIZE = CP.SIZE
    private val SIZE_M = CP.SIZE_M

    init {
        this.w = width
        this.h = height
        this.t = windowTitle
        createWindow()

        ground = Ground(w, h)

        input = Input()
        input.frame = this@GameWindow
        addKeyListener(input)

        initMap()

        player = Tank(input, ground)
        player.w = CP.TANK_SIZE
        player.h = CP.TANK_SIZE
        //player.ground = ground
        player.observer = this
        list.add(player)

        // 注册方向键监听，方便通知玩家坦克的动作
        input.moveListener = player

        darkAI = DarkAI()

        tempImage = this.createImage(w, h)
        tempGraphics = tempImage?.graphics

        setFps(60)
        renderThread = RenderThread(this)
        renderThread?.start()
        // 延迟500ms再启动渲染线程避免一些对象没有初始化完成导致的Exception, 如NPE...
        // 估计还是有问题，list绘制对象容器，添加内容的时机应该也要晚一些才行。
//        Timer().schedule(object : TimerTask() {
//            override fun run() {
//                renderThread?.start()
//            }
//        }, 500L)

        println("Title Height: ${insets.top}")

    }

    /**
     * 使用byte二维数组实现地图布局
     */
    private fun initMap() {
//        [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
//        [1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1]
//        [1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1]
//        [1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1]
//        [1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1]
//        [1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1]
//        [1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1]
//        [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
//        [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
//        [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
//        [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
//        [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
//        [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
//        [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
//        [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        // test map
        val mapArray = CP.mapArray
        val tileArray = CP.tileArray

        // 4个方格代表的一个瓦片（河流、草地、基地...），每个元素存储二维数组的横纵坐标
        val four: MutableList<Int> = ArrayList()
        for (i in four.indices) {
            four[i] = -1
        }

        for (i in mapArray.indices) {
            for (j in mapArray[i].indices) {
                val tile = mapArray[i][j].toInt()
                println("tile:$tile")
                if (tile == CP.TILE_BRICK) {
                    var brick = Brick()
                    brick.id = (i shl 8 or j).toLong()
                    brick.x = SIZE_M * j
                    brick.y = SIZE_M * i
                    brick.w = SIZE_M
                    brick.h = SIZE_M
                    brick.ground = ground
                    brick.observer = this
                    list.add(brick)
                    tileList.add(brick)
                    tileArray[i][j] = brick
                } else if (tile == CP.TILE_IRON) {
                    var iron = Iron()
                    iron.id = (i shl 8 or j).toLong()
                    iron.x = SIZE_M * j
                    iron.y = SIZE_M * i
                    iron.w = SIZE_M
                    iron.h = SIZE_M
                    iron.ground = ground
                    iron.observer = this
                    list.add(iron)
                    tileList.add(iron)
                    tileArray[i][j] = iron
                } else if (tile == CP.TILE_RIVER) {
                    var river = River()
                    river.id = (i shl 8 or j).toLong()
                    river.x = SIZE * j
                    river.y = SIZE * i
                    river.w = SIZE
                    river.h = SIZE
                    river.ground = ground
                    list.add(river)
                    tileList.add(river)
                    tileArray[i][j] = river
                } else if (tile == CP.TILE_GRASS) {
                    var rowCol = i shl 8 or j
                    //如果包含就不再处理，防止每个小格绘制4个瓦片
                    if (four.contains(rowCol)) {
                        continue
                    }

                    var grass = Grass()
                    grass.id = (i shl 8 or j).toLong()
                    grass.x = SIZE_M * j
                    grass.y = SIZE_M * i
                    grass.w = SIZE
                    grass.h = SIZE
                    grass.ground = ground
                    list.add(grass)
                    tileList.add(grass)
                    tileArray[i][j] = grass

                    four.add(0, rowCol)
                    //判断其他3个小格是不是同样是这个瓦片数值(小心数组下标越界)
                    if (mapArray[i][j + 1].toInt() == CP.TILE_GRASS) {
                        var rc = i shl 8 or (j + 1)
                        four.add(1, rc)
                    }
                    if (mapArray[i + 1][j].toInt() == CP.TILE_GRASS) {
                        var rc = (i + 1) shl 8 or j
                        four.add(2, rc)
                    }
                    if (mapArray[i + 1][j + 1].toInt() == CP.TILE_GRASS) {
                        var rc = (i + 1) shl 8 or (j + 1)
                        four.add(3, rc)
                    }
                } else if (tile == CP.TILE_EAGLE) {
                    var rowCol = i shl 8 or j
                    //如果包含就不再处理，防止每个小格绘制4个瓦片
                    if (four.contains(rowCol)) {
                        continue
                    }

                    var eagle = Eagle()
                    eagle.id = (i shl 8 or j).toLong()
                    eagle.x = SIZE_M * j
                    eagle.y = SIZE_M * i
                    eagle.w = SIZE
                    eagle.h = SIZE
                    eagle.ground = ground
                    list.add(eagle)
                    tileList.add(eagle)
                    tileArray[i][j] = eagle

                    four.add(0, rowCol)
                    //判断其他3个小格是不是同样是这个瓦片数值(小心数组下标越界)
                    if (mapArray[i][j + 1].toInt() == CP.TILE_EAGLE) {
                        var rc = i shl 8 or (j + 1)
                        four.add(1, rc)
                    }
                    if (mapArray[i + 1][j].toInt() == CP.TILE_EAGLE) {
                        var rc = (i + 1) shl 8 or j
                        four.add(2, rc)
                    }
                    if (mapArray[i + 1][j + 1].toInt() == CP.TILE_EAGLE) {
                        var rc = (i + 1) shl 8 or (j + 1)
                        four.add(3, rc)
                    }
                }
            }
        }

        // good map
//        val mapArray = Array(CP.R) { ByteArray(CP.C) }
//        for (i in mapArray.indices) {
//            if (i in 6..8) {
//                break
//                //continue
//            }
//            if (i == 14){
//                continue
//            }
//            for (j in mapArray[i].indices) {
//                if (i == 0) {
//                    mapArray[i][j] = 0
//                } else {
//                    if (j % 2 == 0) {
//                        mapArray[i][j] = 1
//
//                        var brick = Brick()
//                        brick.x = SIZE * j
//                        brick.y = SIZE * i
//                        brick.ground = ground
//                        list.add(brick)
//                    }
//                }
//            }
//
//        }
//        for (i in mapArray.indices) {
//            println(Arrays.toString(mapArray[i]))
//        }
    }

    private fun detectCollision() {
//        tileList.forEachIndexed { index, tile ->
//            val tcx = tile.x + tile.w / 2
//            val pcx = player.x + player.w / 2
//            val tpwhalf = (tile.w + player.w) / 2
//
//            val tcy = tile.y + tile.h / 2
//            val pcy = player.y + player.h / 2
//            val tphhalf = (tile.h + player.h) / 2

//            if (Math.abs(tcx - pcx) <= tpwhalf && Math.abs(tcy - pcy) <= tphhalf) {
//                println("--碰撞--${index}")
//                when (player.direction) {
//                    Shells.DIRECTION_NORTH -> {
//                        println("上面撞墙了，边界坐标y:${tile.y + tile.h}")
//                    }
//                    Shells.DIRECTION_SOUTH -> {
//                        println("下面撞墙了，边界坐标y:${tile.y}")
//                    }
//                    Shells.DIRECTION_WEST -> {
//                        println("左面撞墙了，边界坐标x:${tile.x + tile.w}")
//                    }
//                    Shells.DIRECTION_EAST -> {
//                        println("右面撞墙了，边界坐标x:${tile.x}")
//                    }
//                    else -> {}
//                }
//            }
//        }
//        for (tile in tileList) {
//            val tcx = tile.x + tile.w / 2
//            val pcx = player.x + player.w / 2
//            val tpwhalf = (tile.w + player.w) / 2
//
//            val tcy = tile.y + tile.h / 2
//            val pcy = player.y + player.h / 2
//            val tphhalf = (tile.h + player.h) / 2
//
//            if (Math.abs(tcx - pcx) <= tpwhalf && Math.abs(tcy - pcy) <= tphhalf) {
//                println("--碰撞${tile}")
//            }
//        }
    }

    private fun createWindow() {
        setSize(w, h)
        title = t
        isUndecorated = true
        isVisible = true
        setLocationRelativeTo(null)
        defaultCloseOperation = EXIT_ON_CLOSE
    }

    fun setFps(fps: Int): Boolean {
        return if (fps <= 0) {
            false
        } else {
            this.fps = fps
            true
        }
    }

    fun getFps() = fps

    fun clear(g: Graphics?) {
        g?.color = Color.BLACK
        g?.fillRect(0, 0, w, h)
    }

    fun exit() {
        println("Good bye")
    }

    override fun paint(g: Graphics?) {

        clear(tempGraphics)

        for (gameObject in list) {
//            if (gameObject.isDestroyed) {
//                continue
//            }
            gameObject.draw(tempGraphics)
            gameObject.onTick()
        }

        //////////////////方便调试的网格线
//        var color = tempGraphics?.color
//        tempGraphics?.color = Color.GRAY
//        for (i in 0 until CP.R) {
//            tempGraphics?.color = Color.GRAY
//            tempGraphics?.drawLine(0, SIZE * i, w, SIZE * i)
//        }
//        for (j in 0 until CP.C) {
//            tempGraphics?.color = Color.GRAY
//            tempGraphics?.drawLine(SIZE * j, 0, SIZE * j, h)
//        }
//
//        //行列指示器
////        tempGraphics?.color = Color.ORANGE
////        for (i in 0 until CP.R) {
////            for (j in 0 until CP.C) {
////                if (j == 0) {
////                    tempGraphics?.drawString("$i", 0, SIZE * i + 35)
////                }
////                if (i == 0) {
////                    tempGraphics?.drawString("$j", SIZE * j,  50)
////                }
////            }
////        }
//        tempGraphics?.color = color
        ///////////////////end方便调试的网格线
        g?.drawImage(tempImage, 0, 0, null)

        darkAI?.pushTank(ground, this)

        //detectCollision()
    }

    override fun born(go: GameObject?) {
        println("born")
        list.add(go!!)
    }

    override fun die(go: GameObject?) {
        println("die ${go?.javaClass.toString()}")
        for (gameObject in list) {
            if (gameObject.id == go?.id) {
                list.remove(gameObject)
                println("founded====")
                // 将砖块和铁块从地图上去掉
                if (go is Brick || go is Iron) {
                    var i = (go.id shr 8).toInt()
                    var j = (go.id and 0x00000000000000ff).toInt()
                    CP.mapArray[i][j] = 0
                    CP.tileArray[i][j] = null
                }
                break
            }
        }
    }
}