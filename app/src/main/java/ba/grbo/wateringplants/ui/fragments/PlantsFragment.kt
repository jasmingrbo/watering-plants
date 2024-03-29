package ba.grbo.wateringplants.ui.fragments

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.databinding.FragmentPlantsBinding
import ba.grbo.wateringplants.ui.activities.WateringPlantsActivity
import ba.grbo.wateringplants.ui.adapters.PlantAdapter
import ba.grbo.wateringplants.ui.viewmodels.PlantsViewModel
import ba.grbo.wateringplants.util.*
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.transition.MaterialElevationScale
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlantsFragment : Fragment() {
    //region Properties
    private val viewModel: PlantsViewModel by viewModels()
    private lateinit var activity: WateringPlantsActivity
    private lateinit var binding: FragmentPlantsBinding
    private lateinit var clickedCard: MaterialCardView
    //endregion

    //region Overriden methods
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        viewModel.collectFlows()
        val adapter = PlantAdapter { card, plantId ->
            clickedCard = card
            viewModel.onPlantCardClicked(plantId)
        }
        binding = FragmentPlantsBinding.inflate(inflater, container, false).apply {
            plantsRecyclerView.addItemDecoration(
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                    GridSpacingItemDecoration(
                        (plantsRecyclerView.layoutManager as GridLayoutManager).spanCount,
                        resources.getDimension(R.dimen.spacing_between_plant_cards).toInt(),
                        true
                    ) else VerticalGridSpacingItemDecoration(
                    resources.getDimension(R.dimen.spacing_between_plant_cards).toInt(),
                )
            )
            plantsRecyclerView.adapter = adapter
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (viewModel.lastState == PlantState.VIEWING) {
            postponeEnterTransition()
            view.doOnPreDraw {
                startPostponedEnterTransition()
                lifecycleScope.launch {
                    viewModel.onEnterAnimationStart()
                    delay(resources.getInteger(android.R.integer.config_longAnimTime).toLong())
                    viewModel.onEnterAnimationEnd()
                    exitTransition = null
                    reenterTransition = null
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.app_bar_plants, menu)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = requireActivity() as WateringPlantsActivity
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return viewModel.processActionBarItemId(item.itemId)
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        return onCreateAnimation(
            transit,
            enter,
            nextAnim,
            resources.getInteger(android.R.integer.config_mediumAnimTime).toLong(),
            requireContext(),
            lifecycleScope,
            viewModel::onEnterAnimationStart,
            viewModel::onEnterAnimationEnd,
        ) { t, e, a -> super.onCreateAnimation(t, e, a) }
    }
    //endregion

    //region Flow collectors
    private fun PlantsViewModel.collectFlows() {
        collect(plants) { (binding.plantsRecyclerView.adapter as PlantAdapter).submitList(it) }
        collect(addPlantEvent) { addPlant(it) }
        collect(viewPlantEvent) { viewPlant(it) }
        collect(removeGlideCacheEvent) {
            lifecycleScope.launch(Dispatchers.Default) {
                Glide.get(requireContext()).clearDiskCache()
            }
        }
        collect(enterAnimationStartEvent) { this@PlantsFragment.onEnterAnimationStart() }
        collect(enterAnimationEndEvent) { this@PlantsFragment.onEnterAnimationEnd() }
    }
    //endregion

    //region Helper methods
    private fun addPlant(plantState: PlantState) {
        activity.setBottomNavigationVisibility(View.GONE)
        exitTransition = null
        reenterTransition = null
        navigateToPlantFragmentAdding(plantState)
    }

    private fun viewPlant(plantInfo: Pair<PlantState, Int>) {
        activity.setBottomNavigationVisibility(View.GONE)
        exitTransition = MaterialElevationScale(false).apply {
            duration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()

        }
        reenterTransition = MaterialElevationScale(true).apply {
            duration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()

        }
        navigateToPlantFragmentViewing(plantInfo.first, plantInfo.second)
    }

    private fun navigateToPlantFragmentAdding(plantState: PlantState) {
        findNavController().navigate(
            PlantsFragmentDirections.actionPlantsFragmentToPlantFragment(
                plantState,
                -1
            ),
            NavOptions.Builder()
                .setEnterAnim(android.R.anim.slide_in_left)
                .setExitAnim(android.R.anim.fade_out)
                .setPopEnterAnim(android.R.anim.fade_in)
                .setPopExitAnim(android.R.anim.slide_out_right)
                .setLaunchSingleTop(true)
                .build()
        )
    }

    private fun navigateToPlantFragmentViewing(plantState: PlantState, plantId: Int) {
        findNavController().navigate(
            PlantsFragmentDirections.actionPlantsFragmentToPlantFragment(
                plantState,
                plantId
            ),
            FragmentNavigatorExtras(clickedCard to plantId.toString())
        )
    }

    private fun onEnterAnimationStart() {
        activity.setBottomNavigationVisibility(View.VISIBLE)
        disableViews()
    }

    private fun onEnterAnimationEnd() {
        enableViews()
    }

    private fun disableViews() {
        setEnabled(false)
    }

    private fun enableViews() {
        setEnabled(true)
    }

    private fun setEnabled(state: Boolean) {
    }
    //endregion
}