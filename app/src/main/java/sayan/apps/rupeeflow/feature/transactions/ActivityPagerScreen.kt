package sayan.apps.rupeeflow.feature.transactions

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import sayan.apps.rupeeflow.feature.recurring.RecurringTabContent
import sayan.apps.rupeeflow.feature.recurring.RecurringViewModel

enum class ActivityTab(val title: String) {
    Transactions("Transactions"),
    Recurring("Recurring")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityPagerScreen(
    onMenuClick: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToAddRecurring: () -> Unit,
    onNavigateToTransactionDetail: (Long) -> Unit,
    onNavigateToRecurringDetail: (Long) -> Unit,
    initialTab: Int = 0,
    modifier: Modifier = Modifier,
    transactionsViewModel: TransactionsViewModel = hiltViewModel(),
    recurringViewModel: RecurringViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(initialPage = initialTab) { ActivityTab.entries.size }
    val scope = rememberCoroutineScope()
    
    val transactions by transactionsViewModel.transactions.collectAsState()
    val recurringBadgeCount by recurringViewModel.badgeCount.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            AnimatedContent(
                targetState = pagerState.currentPage,
                transitionSpec = {
                    (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                },
                label = "FABAnimation"
            ) { page ->
                FloatingActionButton(
                    onClick = {
                        if (page == 0) onNavigateToAddTransaction() else onNavigateToAddRecurring()
                    },
                    containerColor = Color(0xFF7C3AED),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add", modifier = Modifier.size(24.dp))
                }
            }
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.Black,
                    contentColor = Color.White,
                    modifier = Modifier.width(300.dp),
                    indicator = { 
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(pagerState.currentPage),
                            width = 48.dp,
                            color = Color(0xFF7C3AED),
                            shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                        )
                    },
                    divider = {}
                ) {
                    ActivityTab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tab.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 15.sp
                                        )
                                    )
                                    if (tab == ActivityTab.Recurring && recurringBadgeCount > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            color = Color(0xFF7C3AED),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.sizeIn(minWidth = 16.dp, minHeight = 16.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = recurringBadgeCount.toString(),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.Top
            ) { page ->
                when (ActivityTab.entries[page]) {
                    ActivityTab.Transactions -> {
                        TransactionsListContent(
                            transactions = transactions,
                            onTransactionClick = { onNavigateToTransactionDetail(it.id.toLong()) }
                        )
                    }
                    ActivityTab.Recurring -> {
                        RecurringTabContent(
                            onNavigateToDetail = onNavigateToRecurringDetail,
                            viewModel = recurringViewModel
                        )
                    }
                }
            }
        }
    }
}
