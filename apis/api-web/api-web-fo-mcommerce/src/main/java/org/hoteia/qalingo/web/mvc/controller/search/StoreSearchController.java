/**
 * Most of the code in the Qalingo project is copyrighted Hoteia and licensed
 * under the Apache License Version 2.0 (release version 0.7.0)
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *                   Copyright (c) Hoteia, 2012-2013
 * http://www.hoteia.com - http://twitter.com/hoteia - contact@hoteia.com
 *
 */
package org.hoteia.qalingo.web.mvc.controller.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.hoteia.qalingo.core.Constants;
import org.hoteia.qalingo.core.ModelConstants;
import org.hoteia.qalingo.core.domain.Cart;
import org.hoteia.qalingo.core.domain.MarketArea;
import org.hoteia.qalingo.core.domain.Retailer;
import org.hoteia.qalingo.core.domain.Store;
import org.hoteia.qalingo.core.domain.enumtype.FoUrls;
import org.hoteia.qalingo.core.pojo.RequestData;
import org.hoteia.qalingo.core.service.ProductService;
import org.hoteia.qalingo.core.service.RetailerService;
import org.hoteia.qalingo.core.solr.response.StoreResponseBean;
import org.hoteia.qalingo.core.solr.service.StoreSolrService;
import org.hoteia.qalingo.core.solr.service.impl.AbstractSolrService;
import org.hoteia.qalingo.core.web.mvc.viewbean.CartViewBean;
import org.hoteia.qalingo.core.web.mvc.viewbean.RecentProductViewBean;
import org.hoteia.qalingo.core.web.mvc.viewbean.SearchStoreItemViewBean;
import org.hoteia.qalingo.core.web.servlet.ModelAndViewThemeDevice;
import org.hoteia.qalingo.core.web.servlet.view.RedirectView;
import org.hoteia.qalingo.web.mvc.controller.AbstractMCommerceController;
import org.hoteia.qalingo.web.mvc.form.SearchForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.support.MutableSortDefinition;
import org.springframework.beans.support.PagedListHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * 
 */
@Controller("storeSearchController")
public class StoreSearchController extends AbstractMCommerceController {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	public ProductService productMarketingService;
	
	@Autowired
	public StoreSolrService storeSolrService;

	@RequestMapping(value = FoUrls.STORE_SEARCH_URL, method = RequestMethod.GET)
	public ModelAndView search(final HttpServletRequest request, final HttpServletResponse response, @Valid SearchForm searchForm,
	                           BindingResult result, ModelMap modelMap) throws Exception {
		
		ModelAndViewThemeDevice modelAndView = new ModelAndViewThemeDevice(getCurrentVelocityPath(request), FoUrls.STORE_SEARCH.getVelocityPage());
        final RequestData requestData = requestUtil.getRequestData(request);

		if (result.hasErrors()) {
			return displaySearch(request, response, modelMap);
		}

//		final SearchViewBean search = frontofficeViewBeanFactory.buildViewBeanSearch(requestData);
//		modelAndView.addObject("search", search);
		
		String url = requestUtil.getCurrentRequestUrl(request);
		
		String sessionKey = "PagedListHolder_Search_List_ProductMarketing_" + request.getSession().getId();
        int page = searchForm.getPage() - 1;
        String mode = request.getParameter(Constants.PAGE_VIEW_MODE);
        String cities = request.getParameter("cities");
        String countries = request.getParameter("countries");
        
        List<String> cityList = new ArrayList<String>();
        if (StringUtils.isEmpty(cities)) {
        	cityList = null;
		}else{
			String[] arr = cities.split(",");
			cityList = Arrays.asList(arr);
		}       
        List<String> countryList = new ArrayList<String>();
        if (StringUtils.isEmpty(countries)) {
        	countryList = null;
		}else{
			String[] arr = countries.split(",");
			countryList = Arrays.asList(arr);
		}
        
        
        String sortBy = searchForm.getSortBy();
        String order = searchForm.getOrder();
		
		try {
			StoreResponseBean storeResponseBean = null;
			List<String> facetFields = Arrays.asList(StoreResponseBean.STORE_DEFAULT_FACET_FIELD,StoreResponseBean.STORE_SECOND_FACET_FIELD);
			storeResponseBean = storeSolrService.searchStore(StoreResponseBean.STORE_DEFAULT_SEARCH_FIELD, searchForm.getText(), facetFields,
					 cityList,countryList);
			StoreResponseBean storeResponBeanNonFilter = storeSolrService.searchStore(StoreResponseBean.STORE_DEFAULT_SEARCH_FIELD, searchForm.getText(),facetFields);
			
			modelAndView.addObject(AbstractSolrService.SEARCH_FACET_FIELD_LIST, frontofficeViewBeanFactory.buildListViewBeanStoreSearchFacet(requestData, storeResponBeanNonFilter));
	        
			PagedListHolder<SearchStoreItemViewBean> productsViewBeanPagedListHolder;

			productsViewBeanPagedListHolder = initList(request, sessionKey, storeResponseBean, new PagedListHolder<SearchStoreItemViewBean>(), searchForm);
	        
	        int pageCurrent = productsViewBeanPagedListHolder.getPage();
	        if (pageCurrent < page) { 
	        	for (int i = pageCurrent; i < page; i++) {
	        		productsViewBeanPagedListHolder.nextPage(); 
				}
	        } else if (pageCurrent > page) { 
	        	for (int i = page; i < pageCurrent; i++) {
		        	productsViewBeanPagedListHolder.previousPage(); 
				}
	        }
	        
			modelAndView.addObject(Constants.PAGINATION_PAGE_URL, url);
			modelAndView.addObject(Constants.PAGINATION_PAGE_PAGED_LIST_HOLDER, productsViewBeanPagedListHolder);
			modelAndView.addObject(Constants.SEARCH_TEXT, searchForm.getText());
			modelAndView.addObject(Constants.PAGINATION_PAGE_SIZE, productsViewBeanPagedListHolder.getPageSize());
			modelAndView.addObject(Constants.PAGINATION_SORT_BY, sortBy);
			modelAndView.addObject(Constants.PAGINATION_ORDER, order);
//			modelAndView.addObject(Constants.PRICE_RANGE_PARAMETER, searchForm.getPrice());
			modelAndView.addObject(Constants.STORE_CITY_PARAMETER, cities);
			modelAndView.addObject(Constants.STORE_COUNTRY_PARAMETER, countries);
		} catch (Exception e) {
			logger.error("SOLR Error", e);
			return displaySearch(request, response, modelMap);
		}
		
		final List<String> listId = requestUtil.getRecentProductSkuCodesFromCookie(request);
        List<RecentProductViewBean> recentProductViewBeans = frontofficeViewBeanFactory.buildListViewBeanRecentProduct(requestData, listId);
        modelAndView.addObject(ModelConstants.RECENT_PPRODUCT_MARKETING_VIEW_BEAN, recentProductViewBeans);
        
        final Cart currentCart = requestData.getCart();
        final CartViewBean cartViewBean = frontofficeViewBeanFactory.buildViewBeanCart(requestUtil.getRequestData(request), currentCart);
        modelAndView.addObject(ModelConstants.CART_VIEW_BEAN, cartViewBean);
		
        overrideDefaultSeoPageTitleAndMainContentTitle(request, modelAndView, FoUrls.STORE_SEARCH.getKey());

        return modelAndView;
	}

    protected ModelAndView displaySearch(final HttpServletRequest request, final HttpServletResponse response, ModelMap modelMap) throws Exception {
        ModelAndViewThemeDevice modelAndView = new ModelAndViewThemeDevice(getCurrentVelocityPath(request), FoUrls.STORE_SEARCH.getVelocityPage());
        final RequestData requestData = requestUtil.getRequestData(request);

//        final SearchViewBean search = frontofficeViewBeanFactory.buildViewBeanSearch(requestData);
//        modelAndView.addObject("search", search);

        modelAndView.addObject("searchForm", formFactory.buildSearchForm(requestData));

        final List<String> listId = requestUtil.getRecentProductSkuCodesFromCookie(request);
        List<RecentProductViewBean> recentProductViewBeans = frontofficeViewBeanFactory.buildListViewBeanRecentProduct(requestData, listId);
        modelAndView.addObject(ModelConstants.RECENT_PPRODUCT_MARKETING_VIEW_BEAN, recentProductViewBeans);

        final Cart currentCart = requestData.getCart();
        final CartViewBean cartViewBean = frontofficeViewBeanFactory.buildViewBeanCart(requestUtil.getRequestData(request), currentCart);
        modelAndView.addObject(ModelConstants.CART_VIEW_BEAN, cartViewBean);

        return modelAndView;
    }
	
    // TODO : Temporary
    
    @Autowired
    public RetailerService retailerService;

    @RequestMapping(value = "/**/search-load-store-index.html", method = RequestMethod.GET)
    public ModelAndView loadIndex(final HttpServletRequest request, final HttpServletResponse response, ModelMap modelMap) throws Exception {
        final RequestData requestData = requestUtil.getRequestData(request);
        final MarketArea marketArea = requestData.getMarketArea();

        List<Retailer> retailers = retailerService.findAllRetailers();;
        
        for (Retailer retailer : retailers) {
            List<Store> stores = retailerService.findStoresByRetailerId(retailer.getId());
            for (Iterator<Store> iterator = stores.iterator(); iterator.hasNext();) {
                Store store = (Store) iterator.next();
                storeSolrService.addOrUpdateStore(store, marketArea);
            }
		}

        return new ModelAndView(new RedirectView(urlService.generateUrl(FoUrls.STORE_SEARCH, requestUtil.getRequestData(request))));
    }
	
	private PagedListHolder<SearchStoreItemViewBean> initList(final HttpServletRequest request, final String sessionKey, final StoreResponseBean storeResponseBean,
			PagedListHolder<SearchStoreItemViewBean> productsViewBeanPagedListHolder, final SearchForm searchForm) throws Exception{
		int pageSize = searchForm.getPageSize();
		String sortBy = searchForm.getSortBy();
        String order = searchForm.getOrder();
		List<SearchStoreItemViewBean> searchtItems = frontofficeViewBeanFactory.buildListViewBeanSearchStoreItem(requestUtil.getRequestData(request), storeResponseBean);
		productsViewBeanPagedListHolder = new PagedListHolder<SearchStoreItemViewBean>(searchtItems);
		productsViewBeanPagedListHolder.setPageSize(pageSize);
		productsViewBeanPagedListHolder.setSort(new MutableSortDefinition(sortBy, true, Constants.PAGE_ORDER_ASC.equalsIgnoreCase(order)));
		productsViewBeanPagedListHolder.resort();
        request.getSession().setAttribute(sessionKey, searchtItems);
        return productsViewBeanPagedListHolder;
	}
	
}