# NetworkMonitor

## This is not the code I wrote, I'm just fount out this on stackoverflow and I thought it gonna be easy to used as a library. 
## So all the credit goes to [Paul Spiesberger](https://stackoverflow.com/users/1254514/paul-spiesberger)
## And this is the [link](https://stackoverflow.com/a/58960879/9056898) of the stackoverflow

### if you want to used it, here is how it is.
[![](https://jitpack.io/v/htueko/NetworkMonitor.svg)](https://jitpack.io/#htueko/NetworkMonitor)

at **project level gradle file**

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
  
 at **module level gradle file**
 
    dependencies {
            implementation 'com.github.htueko:NetworkMonitor:1.0'
    }
  
#### Usage

register network 

    @ExperimentalCoroutinesApi
    @HiltAndroidApp
    class MyApplication : Application(){
        override fun onCreate() {
            super.onCreate()
            GlobalScope.launch {
                NetworkMonitor.getInstance(this@MyApplication).watchNetworkAsLiveData()
            }
        }
    }
    
You can also used\
watchNetwork()\
watchNetworkAsLiveData()\
watchWifi()\
watchWifiAsLiveData()\
watchCellular()\
watchCellularAsLiveData()\
watchEthernet()\
watchEthernetAsLiveData()
  
Then you can observe or collect the data from your application.

I used **Dagger Hilt** for dependency injection.

    @Module
    @InstallIn(ApplicationComponent::class)
    object AppModule {

        // to provide the network monitor
        @ExperimentalCoroutinesApi
        @Singleton
        @Provides
        fun provideNetworkMonitor(
            app: Application
        ) = NetworkMonitor.getInstance(app)

    }
    
So at your Activity/Fragment

    @AndroidEntryPoint
    class MainActivity : AppCompatActivity() {

        @Inject
        lateinit var network: NetworkMonitor

        private val activityScope = CoroutineScope(Dispatchers.Main)

        @ExperimentalCoroutinesApi
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            activityScope.launch {
                network.watchNetworkAsLiveData().observe(this@MainActivity , Observer {
                    if (it) tv_text.text = "Online" else tv_text.text = "Offline"
                })
            }

        }

    }

And there are other feature too. check out the source.

  
