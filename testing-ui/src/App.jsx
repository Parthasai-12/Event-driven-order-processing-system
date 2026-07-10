import React, { useState, useEffect } from 'react'

function App() {
  // Service base URLs
  const ORDER_SERVICE_URL = 'http://localhost:8081'
  const INVENTORY_SERVICE_URL = 'http://localhost:8082'
  const PAYMENT_SERVICE_URL = 'http://localhost:8083'
  const NOTIFICATION_SERVICE_URL = 'http://localhost:8084'

  // Application States
  const [products, setProducts] = useState([])
  const [notifications, setNotifications] = useState([])
  const [createdOrderId, setCreatedOrderId] = useState('')
  const [selectedOrderId, setSelectedOrderId] = useState('')
  const [orderDetail, setOrderDetail] = useState(null)
  
  // Simulation Toggle states (for display status check)
  const [inventoryFailureMode, setInventoryFailureMode] = useState(false)
  const [paymentFailureMode, setPaymentFailureMode] = useState(false)

  // Create Order Form State
  const [orderForm, setOrderForm] = useState({
    productId: '',
    quantity: 1,
    amount: 100,
    simulateInventoryFailure: false,
    simulatePaymentFailure: false
  })

  // Messages
  const [message, setMessage] = useState({ text: '', isError: false })

  const showMessage = (text, isError = false) => {
    setMessage({ text, isError })
    setTimeout(() => setMessage({ text: '', isError: false }), 5000)
  }

  // Fetch Catalog
  const fetchProducts = async () => {
    try {
      const response = await fetch(`${INVENTORY_SERVICE_URL}/inventory/products`)
      if (!response.ok) throw new Error('Failed to fetch catalog')
      const data = await response.json()
      setProducts(data)
    } catch (error) {
      console.error('Error fetching inventory:', error)
    }
  }

  // Fetch Notifications
  const fetchNotifications = async () => {
    try {
      const response = await fetch(`${NOTIFICATION_SERVICE_URL}/notifications`)
      if (!response.ok) throw new Error('Failed to fetch notifications')
      const data = await response.json()
      setNotifications(data)
    } catch (error) {
      console.error('Error fetching notifications:', error)
    }
  }

  // Initial loads and background polling
  useEffect(() => {
    fetchProducts()
    fetchNotifications()

    // Poll notifications every 3 seconds to show background Kafka events arriving
    const interval = setInterval(fetchNotifications, 3000)
    return () => clearInterval(interval)
  }, [])

  // Poll Order Status when selectedOrderId is set
  useEffect(() => {
    if (!selectedOrderId) {
      setOrderDetail(null)
      return
    }

    const fetchOrderStatus = async () => {
      try {
        const response = await fetch(`${ORDER_SERVICE_URL}/orders/${selectedOrderId}`)
        if (response.ok) {
          const data = await response.json()
          setOrderDetail(data)
        } else {
          setOrderDetail(null)
        }
      } catch (error) {
        console.error('Error fetching order status:', error)
      }
    }

    fetchOrderStatus() // Initial fetch

    const interval = setInterval(fetchOrderStatus, 3000)
    return () => clearInterval(interval)
  }, [selectedOrderId])

  // Select a product from catalog to autofill form
  const handleBuyClick = (product) => {
    setOrderForm({
      productId: product.productId.toString(),
      quantity: 1,
      amount: 100, // standard mock amount, user can change
      simulateInventoryFailure: false,
      simulatePaymentFailure: false
    })
    showMessage(`Selected product ${product.productName} (ID: ${product.productId})`)
  }

  // Submit Order Creation
  const handleCreateOrder = async (e) => {
    e.preventDefault()
    if (!orderForm.productId || orderForm.quantity <= 0 || orderForm.amount <= 0) {
      showMessage('Please provide valid product ID, quantity, and amount', true)
      return
    }

    try {
      const payload = {
        productId: parseInt(orderForm.productId),
        quantity: parseInt(orderForm.quantity),
        amount: parseFloat(orderForm.amount),
        simulateInventoryFailure: orderForm.simulateInventoryFailure,
        simulatePaymentFailure: orderForm.simulatePaymentFailure
      }

      const response = await fetch(`${ORDER_SERVICE_URL}/orders`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      })

      if (!response.ok) {
        throw new Error('Failed to submit order')
      }

      const data = await response.json()
      setCreatedOrderId(data.id)
      setSelectedOrderId(data.id) // Automount in Order Status checking section
      showMessage(`Order #${data.id} created successfully!`)
      fetchProducts() // Refresh catalog
    } catch (error) {
      showMessage(error.message, true)
    }
  }

  // Toggle Failure Mode (Scenario 5 testing helper)
  const toggleFailureMode = async (service, currentVal, setVal, url) => {
    try {
      const targetVal = !currentVal
      const response = await fetch(url, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ enabled: targetVal })
      })

      if (!response.ok) throw new Error(`Failed to configure failure mode on ${service}`)
      setVal(targetVal)
      showMessage(`${service} Failure Mode set to ${targetVal ? 'ENABLED' : 'DISABLED'}`)
    } catch (error) {
      showMessage(error.message, true)
    }
  }

  return (
    <div style={{ padding: '20px', fontFamily: 'sans-serif', maxWidth: '1200px', margin: '0 auto' }}>
      <header style={{ borderBottom: '1px solid #ccc', paddingBottom: '10px', marginBottom: '20px' }}>
        <h2>Event-Driven Platform Testing UI</h2>
        <p style={{ color: '#666', fontSize: '14px' }}>
          Minimal test control panel to place orders, simulate failures, and monitor compensating Saga transactions.
        </p>
      </header>

      {/* Message Notifications */}
      {message.text && (
        <div style={{
          padding: '10px',
          marginBottom: '15px',
          borderRadius: '4px',
          backgroundColor: message.isError ? '#ffe6e6' : '#e6ffe6',
          border: `1px solid ${message.isError ? '#ff8080' : '#80ff80'}`,
          color: message.isError ? '#cc0000' : '#006600'
        }}>
          {message.text}
        </div>
      )}

      {/* Failure Mode API Control Panel */}
      <section style={{ border: '1px solid #ddd', padding: '15px', borderRadius: '8px', marginBottom: '20px', backgroundColor: '#f9f9f9' }}>
        <h4 style={{ margin: '0 0 10px 0' }}>Failure Simulation (Retry & DLQ Controls)</h4>
        <div style={{ display: 'flex', gap: '15px' }}>
          <button 
            type="button"
            onClick={() => toggleFailureMode(
              'Inventory Service', 
              inventoryFailureMode, 
              setInventoryFailureMode, 
              `${INVENTORY_SERVICE_URL}/inventory/failure-mode`
            )}
            style={{
              padding: '8px 12px',
              backgroundColor: inventoryFailureMode ? '#d9534f' : '#337ab7',
              color: '#fff',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer'
            }}
          >
            {inventoryFailureMode ? 'Disable Inventory Failure Mode' : 'Enable Inventory Failure Mode (Throws error)'}
          </button>
          
          <button 
            type="button"
            onClick={() => toggleFailureMode(
              'Payment Service', 
              paymentFailureMode, 
              setPaymentFailureMode, 
              `${PAYMENT_SERVICE_URL}/payments/failure-mode`
            )}
            style={{
              padding: '8px 12px',
              backgroundColor: paymentFailureMode ? '#d9534f' : '#337ab7',
              color: '#fff',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer'
            }}
          >
            {paymentFailureMode ? 'Disable Payment Failure Mode' : 'Enable Payment Failure Mode (Throws error)'}
          </button>
        </div>
        <p style={{ margin: '8px 0 0 0', fontSize: '12px', color: '#666' }}>
          Enabling these toggles causes the microservice to throw exceptions during event processing, demonstrating retries (1s, 2s, 4s) and routing to DLQ.
        </p>
      </section>

      <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>
        {/* left column: Catalog & Form */}
        <div style={{ flex: '1', minWidth: '350px' }}>
          {/* Section 1: Product Catalog */}
          <section style={{ border: '1px solid #ddd', padding: '15px', borderRadius: '8px', marginBottom: '20px' }}>
            <h3>1. Product Catalog</h3>
            <button onClick={fetchProducts} style={{ marginBottom: '10px' }}>Refresh Catalog</button>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {products.length === 0 ? (
                <p style={{ fontSize: '13px', color: '#999' }}>No catalog records. Start services to load.</p>
              ) : (
                products.map((product) => (
                  <div key={product.productId} style={{ border: '1px dashed #ccc', padding: '10px', borderRadius: '4px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                      <strong>{product.productName}</strong><br/>
                      <span style={{ fontSize: '12px', color: '#555' }}>
                        ID: {product.productId} | Stock: {product.availableQuantity} | Reserved: {product.reservedQuantity}
                      </span>
                    </div>
                    <button 
                      type="button" 
                      onClick={() => handleBuyClick(product)}
                      style={{ padding: '4px 8px', cursor: 'pointer' }}
                    >
                      Buy
                    </button>
                  </div>
                ))
              )}
            </div>
          </section>

          {/* Section 2: Create Order Form */}
          <section style={{ border: '1px solid #ddd', padding: '15px', borderRadius: '8px' }}>
            <h3>2. Create Order</h3>
            <form onSubmit={handleCreateOrder}>
              <div style={{ marginBottom: '10px' }}>
                <label style={{ display: 'block', marginBottom: '4px' }}>Product ID:</label>
                <input 
                  type="text" 
                  value={orderForm.productId}
                  onChange={(e) => setOrderForm({ ...orderForm, productId: e.target.value })}
                  style={{ width: '100%', padding: '6px', boxSizing: 'border-box' }}
                  placeholder="e.g. 1"
                />
              </div>

              <div style={{ marginBottom: '10px' }}>
                <label style={{ display: 'block', marginBottom: '4px' }}>Quantity:</label>
                <input 
                  type="number" 
                  value={orderForm.quantity}
                  onChange={(e) => setOrderForm({ ...orderForm, quantity: parseInt(e.target.value) || 1 })}
                  style={{ width: '100%', padding: '6px', boxSizing: 'border-box' }}
                  min="1"
                />
              </div>

              <div style={{ marginBottom: '10px' }}>
                <label style={{ display: 'block', marginBottom: '4px' }}>Amount:</label>
                <input 
                  type="number" 
                  value={orderForm.amount}
                  onChange={(e) => setOrderForm({ ...orderForm, amount: parseFloat(e.target.value) || 0 })}
                  style={{ width: '100%', padding: '6px', boxSizing: 'border-box' }}
                  min="1"
                />
              </div>

              {/* Simulation flags */}
              <div style={{ marginBottom: '10px', border: '1px solid #ffebeb', padding: '8px', borderRadius: '4px', backgroundColor: '#fffcfc' }}>
                <strong>Simulation Flags:</strong>
                <div style={{ margin: '8px 0' }}>
                  <label style={{ cursor: 'pointer' }}>
                    <input 
                      type="checkbox" 
                      checked={orderForm.simulateInventoryFailure}
                      onChange={(e) => setOrderForm({ ...orderForm, simulateInventoryFailure: e.target.checked })}
                    />
                    Simulate Inventory Failure (Out of stock)
                  </label>
                </div>
                <div>
                  <label style={{ cursor: 'pointer' }}>
                    <input 
                      type="checkbox" 
                      checked={orderForm.simulatePaymentFailure}
                      onChange={(e) => setOrderForm({ ...orderForm, simulatePaymentFailure: e.target.checked })}
                    />
                    Simulate Payment Failure (Saga Rollback)
                  </label>
                </div>
              </div>

              <button 
                type="submit" 
                style={{ width: '100%', padding: '8px', cursor: 'pointer', backgroundColor: '#4CAF50', color: 'white', border: 'none', borderRadius: '4px' }}
              >
                Place Order
              </button>
            </form>

            {createdOrderId && (
              <div style={{ marginTop: '12px', padding: '8px', background: '#eee', borderRadius: '4px' }}>
                Last Submitted Order ID: <strong>{createdOrderId}</strong>
              </div>
            )}
          </section>
        </div>

        {/* Right column: Status Monitor & Notifications */}
        <div style={{ flex: '1.2', minWidth: '400px' }}>
          {/* Section 3: Order Status */}
          <section style={{ border: '1px solid #ddd', padding: '15px', borderRadius: '8px', marginBottom: '20px' }}>
            <h3>3. Order Status Monitor (Auto polls)</h3>
            <div style={{ display: 'flex', gap: '10px', marginBottom: '15px' }}>
              <input 
                type="text" 
                value={selectedOrderId}
                onChange={(e) => setSelectedOrderId(e.target.value)}
                style={{ flex: '1', padding: '6px' }}
                placeholder="Enter Order ID to monitor status"
              />
              <button onClick={() => setSelectedOrderId('')}>Clear</button>
            </div>

            {selectedOrderId ? (
              orderDetail ? (
                <div style={{ border: '1px dashed #bbb', padding: '12px', borderRadius: '4px', backgroundColor: '#fcfcfc' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                    <span>Order ID: <strong>{orderDetail.id}</strong></span>
                    <span>Product ID: <strong>{orderDetail.productId}</strong></span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                    <span>Quantity: <strong>{orderDetail.quantity}</strong></span>
                    <span>Amount: <strong>${orderDetail.amount}</strong></span>
                  </div>
                  <div style={{ 
                    marginTop: '10px', 
                    padding: '8px', 
                    textAlign: 'center', 
                    borderRadius: '4px',
                    backgroundColor: 
                      orderDetail.status === 'COMPLETED' ? '#d4edda' :
                      orderDetail.status === 'FAILED' ? '#f8d7da' : '#fff3cd',
                    color: 
                      orderDetail.status === 'COMPLETED' ? '#155724' :
                      orderDetail.status === 'FAILED' ? '#721c24' : '#856404',
                    border: '1px solid transparent',
                    fontWeight: 'bold'
                  }}>
                    Current Status: {orderDetail.status}
                  </div>
                  <p style={{ fontSize: '11px', color: '#999', margin: '8px 0 0 0', textAlign: 'center' }}>
                    Polling status every 3 seconds...
                  </p>
                </div>
              ) : (
                <p style={{ fontSize: '13px', color: '#777' }}>Searching for Order #{selectedOrderId}...</p>
              )
            ) : (
              <p style={{ fontSize: '13px', color: '#999' }}>Enter or select an Order ID to poll status changes.</p>
            )}
          </section>

          {/* Section 4: Notifications delivery logs */}
          <section style={{ border: '1px solid #ddd', padding: '15px', borderRadius: '8px' }}>
            <h3>4. Notification Delivery Logs (Auto polls)</h3>
            <button onClick={fetchNotifications} style={{ marginBottom: '10px' }}>Refresh Manual</button>
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12px', textAlign: 'left' }}>
                <thead>
                  <tr style={{ backgroundColor: '#eee', borderBottom: '1px solid #ccc' }}>
                    <th style={{ padding: '8px' }}>Order ID</th>
                    <th style={{ padding: '8px' }}>Event</th>
                    <th style={{ padding: '8px' }}>Message Details</th>
                    <th style={{ padding: '8px' }}>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {notifications.length === 0 ? (
                    <tr>
                      <td colSpan="4" style={{ padding: '10px', textAlign: 'center', color: '#999' }}>
                        No delivery logs yet.
                      </td>
                    </tr>
                  ) : (
                    notifications.map((n) => (
                      <tr key={n.id} style={{ borderBottom: '1px solid #eee' }}>
                        <td style={{ padding: '8px' }}>{n.orderId}</td>
                        <td style={{ padding: '8px' }}>
                          <span style={{
                            padding: '2px 6px',
                            borderRadius: '3px',
                            backgroundColor: n.eventType === 'PAYMENT_SUCCESS' ? '#e6ffe6' : '#ffe6e6',
                            color: n.eventType === 'PAYMENT_SUCCESS' ? '#006600' : '#cc0000',
                            fontWeight: '500'
                          }}>
                            {n.eventType}
                          </span>
                        </td>
                        <td style={{ padding: '8px' }}>{n.message}</td>
                        <td style={{ padding: '8px', color: 'green', fontWeight: 'bold' }}>{n.status}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      </div>
    </div>
  )
}

export default App
